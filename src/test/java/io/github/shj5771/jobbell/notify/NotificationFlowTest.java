package io.github.shj5771.jobbell.notify;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.common.MdcKeys;
import io.github.shj5771.jobbell.common.TraceIds;
import io.github.shj5771.jobbell.guild.Guild;
import io.github.shj5771.jobbell.guild.GuildCompany;
import io.github.shj5771.jobbell.guild.GuildCompanyRepository;
import io.github.shj5771.jobbell.guild.GuildRepository;
import io.github.shj5771.jobbell.guild.MemberFilter;
import io.github.shj5771.jobbell.guild.MemberFilterRepository;
import io.github.shj5771.jobbell.posting.CatalogLoader;
import io.github.shj5771.jobbell.posting.CollectResult;
import io.github.shj5771.jobbell.posting.CollectService;
import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.support.Database;
import io.github.shj5771.jobbell.support.LogCapture;
import io.github.shj5771.jobbell.support.MutableClock;
import io.github.shj5771.jobbell.support.RecordingDiscordClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 수집 → 발송 계획 → 비동기 발송 전 구간. 디스코드는 기록용 클라이언트로 바꿔 끼웠다. */
@IntegrationTest
class NotificationFlowTest {

    private static final long GUILD = 1L;
    private static final long CHANNEL = 10L;
    private static final long FAN = 100L;
    private static final long NOT_FAN = 200L;

    @Autowired
    CollectService collectService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    PendingNotificationSweeper sweeper;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    GuildRepository guilds;

    @Autowired
    GuildCompanyRepository guildCompanies;

    @Autowired
    MemberFilterRepository filters;

    @Autowired
    CatalogLoader catalog;

    @Autowired
    RecordingDiscordClient discord;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

    LogCapture logs;

    @BeforeEach
    void setUp() {
        Database.clean(jdbc);
        discord.reset();
        logs = LogCapture.attachTo("io.github.shj5771.jobbell");

        Guild guild = new Guild(GUILD, clock.instant());
        guild.setAlertChannel(CHANNEL);
        guilds.save(guild);
        guildCompanies.save(new GuildCompany(GUILD, acme().getId(), clock.instant()));
        filters.save(new MemberFilter(GUILD, FAN, "백엔드", "신입"));
        filters.save(new MemberFilter(GUILD, NOT_FAN, "디자이너", null));

        // 기준선 수집. 이때 보인 공고는 알리지 않는다.
        notificationService.dispatch(collectService.collectAll());
        clock.advance(Duration.ofMinutes(2));
    }

    @AfterEach
    void tearDown() {
        logs.close();
    }

    @Test
    @DisplayName("기준선 이후 새 공고만 알린다. 조건이 맞는 사람만 멘션하고, 추적 코드는 수집의 traceId 다")
    void 새_공고를_알린다() {
        assertThat(discord.sent()).as("기준선 공고는 보내지 않는다").isEmpty();

        List<CollectResult> results = collectService.collectAll();
        notificationService.dispatch(results);

        Notification sent = awaitStatus(Notification.Status.SENT);
        RecordingDiscordClient.Sent message = discord.sent().getFirst();
        String collectTraceId = results.stream()
                .filter(r -> r.company().getSourceKey().equals("acme")).findFirst().orElseThrow().traceId();

        assertThat(message.channelId()).isEqualTo(CHANNEL);
        assertThat((String) message.message().get("content"))
                .contains("가짜전자")
                .contains("<@" + FAN + ">")
                .doesNotContain("<@" + NOT_FAN + ">");
        assertThat(footer(message)).isEqualTo("추적 코드 " + TraceIds.shortCode(collectTraceId));
        assertThat(sent.getTraceId()).isEqualTo(collectTraceId);
        assertThat(sent.getDiscordMessageId()).isNotNull();
    }

    @Test
    @DisplayName("발송은 다른 스레드에서 돌지만 로그의 traceId·guildId·postingId 는 이어진다")
    void 비동기_발송_로그() {
        List<CollectResult> results = collectService.collectAll();
        notificationService.dispatch(results);
        Notification sent = awaitStatus(Notification.Status.SENT);

        ILoggingEvent sendLog = logs.awaitEvent(e -> e.getFormattedMessage().startsWith("알림 발송"), Duration.ofSeconds(5));
        assertThat(sendLog.getThreadName()).isNotEqualTo(Thread.currentThread().getName());
        assertThat(sendLog.getMDCPropertyMap())
                .containsEntry(MdcKeys.TRACE_ID, sent.getTraceId())
                .containsEntry(MdcKeys.GUILD_ID, String.valueOf(GUILD))
                .containsEntry(MdcKeys.POSTING_ID, String.valueOf(sent.getPostingId()));
    }

    @Test
    @DisplayName("같은 결과로 두 번 돌려도 메시지는 한 번이다")
    void 중복_발송_없음() {
        List<CollectResult> results = collectService.collectAll();
        notificationService.dispatch(results);
        awaitStatus(Notification.Status.SENT);
        notificationService.dispatch(results);

        assertThat(notifications.count()).isEqualTo(1);
        assertThat(discord.sent()).hasSize(1);
        assertThat(logs.events()).anyMatch(e -> e.getFormattedMessage().contains("이미 발송 기록이 있다"));
    }

    @Test
    @DisplayName("429 를 받으면 기다렸다 다시 보낸다")
    void 호출_한도() {
        discord.rateLimitNext();
        notificationService.dispatch(collectService.collectAll());

        Notification sent = awaitStatus(Notification.Status.SENT);
        assertThat(sent.getAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("실패하면 PENDING 으로 남고 점검 때 다시 보낸다. 세 번 실패하면 포기하고 FAILED 로 남긴다")
    void 실패와_재시도() {
        discord.failNext(3);
        notificationService.dispatch(collectService.collectAll());
        Notification pending = awaitAttempts(1);
        assertThat(pending.getStatus()).isEqualTo(Notification.Status.PENDING);

        clock.advance(PendingNotificationSweeper.GRACE.plusSeconds(1));
        sweeper.sweep();
        sweeper.sweep();

        Notification failed = notifications.findById(pending.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(Notification.Status.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(3);
        assertThat(discord.sent()).isEmpty();
    }

    @Test
    @DisplayName("알림 채널이 없으면 보내지 않고, 그 이유를 로그로 남긴다")
    void 채널_없음() {
        Guild guild = guilds.findById(GUILD).orElseThrow();
        guild.setAlertChannel(null);
        guilds.save(guild);

        notificationService.dispatch(collectService.collectAll());

        assertThat(notifications.count()).isZero();
        assertThat(logs.events()).anyMatch(e -> e.getFormattedMessage().contains("알림 채널이 정해지지 않은 서버"));
    }

    private Company acme() {
        return catalog.catalogCompanies().stream().filter(c -> c.getSourceKey().equals("acme")).findFirst().orElseThrow();
    }

    private Notification awaitStatus(Notification.Status status) {
        return await(() -> notifications.findAll().stream().filter(n -> n.getStatus() == status).findFirst().orElse(null));
    }

    private Notification awaitAttempts(int attempts) {
        return await(() -> notifications.findAll().stream().filter(n -> n.getAttempts() >= attempts).findFirst().orElse(null));
    }

    private static Notification await(java.util.function.Supplier<Notification> probe) {
        Instant deadline = Instant.now().plusSeconds(5);
        while (Instant.now().isBefore(deadline)) {
            Notification found = probe.get();
            if (found != null) {
                return found;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("제한 시간 안에 발송 상태가 바뀌지 않았다");
    }

    @SuppressWarnings("unchecked")
    private static String footer(RecordingDiscordClient.Sent sent) {
        Map<String, Object> embed = ((List<Map<String, Object>>) sent.message().get("embeds")).getFirst();
        return (String) ((Map<String, Object>) embed.get("footer")).get("text");
    }
}
