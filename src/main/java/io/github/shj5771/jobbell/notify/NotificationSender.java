package io.github.shj5771.jobbell.notify;

import io.github.shj5771.jobbell.common.MdcKeys;
import io.github.shj5771.jobbell.common.MdcScope;
import io.github.shj5771.jobbell.common.TraceIds;
import io.github.shj5771.jobbell.discord.DiscordApiException;
import io.github.shj5771.jobbell.discord.DiscordClient;
import io.github.shj5771.jobbell.discord.RateLimitedException;
import io.github.shj5771.jobbell.guild.Guild;
import io.github.shj5771.jobbell.guild.GuildRepository;
import io.github.shj5771.jobbell.guild.MemberFilter;
import io.github.shj5771.jobbell.guild.MemberFilterRepository;
import io.github.shj5771.jobbell.monitor.OpsAlerter;
import io.github.shj5771.jobbell.posting.Posting;
import io.github.shj5771.jobbell.posting.PostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * 발송 1건을 디스코드로 보낸다. {@link #sendAsync} 는 작업 스레드에서 돈다.
 *
 * <p>호출한 쪽(수집)의 MDC 는 {@code AsyncConfig} 의 데코레이터가 옮겨준다. 그래도 여기서 traceId 를
 * 발송 행의 값으로 다시 넣는다 — 재시작 후 점검 스케줄러가 다시 보낼 때는 물려받을 부모가 없기 때문이다.
 */
@Component
public class NotificationSender {

    static final int MAX_ATTEMPTS = 3;

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private final NotificationRepository notifications;
    private final GuildRepository guilds;
    private final PostingRepository postings;
    private final MemberFilterRepository filters;
    private final DiscordClient discord;
    private final TransactionTemplate transaction;
    private final OpsAlerter alerter;
    private final Clock clock;

    public NotificationSender(NotificationRepository notifications, GuildRepository guilds, PostingRepository postings,
                              MemberFilterRepository filters, DiscordClient discord, TransactionTemplate transaction,
                              OpsAlerter alerter, Clock clock) {
        this.notifications = notifications;
        this.guilds = guilds;
        this.postings = postings;
        this.filters = filters;
        this.discord = discord;
        this.transaction = transaction;
        this.alerter = alerter;
        this.clock = clock;
    }

    @Async
    public void sendAsync(long notificationId) {
        send(notificationId);
    }

    public void send(long notificationId) {
        try (MdcScope ignored = MdcScope.open()) {
            Notification notification = notifications.findById(notificationId).orElseThrow();
            MDC.put(MdcKeys.TRACE_ID, notification.getTraceId());
            MDC.put(MdcKeys.GUILD_ID, String.valueOf(notification.getGuildId()));
            MDC.put(MdcKeys.POSTING_ID, String.valueOf(notification.getPostingId()));

            if (notification.getStatus() != Notification.Status.PENDING) {
                log.debug("이미 처리된 발송: {}", notification.getStatus());
                return;
            }
            Guild guild = guilds.findById(notification.getGuildId()).orElseThrow();
            Map<String, Object> message = transaction.execute(status -> buildMessage(notification));

            while (true) {
                notification.attempt();
                try {
                    long messageId = discord.createMessage(guild.getAlertChannelId(), message);
                    notification.sent(messageId, clock.instant());
                    notifications.save(notification);
                    log.info("알림 발송: 채널 {} 메시지 {} ({}번째 시도)", guild.getAlertChannelId(), messageId,
                            notification.getAttempts());
                    return;
                } catch (RateLimitedException e) {
                    if (giveUpIfExhausted(notification, e)) {
                        return;
                    }
                    log.warn("디스코드 호출 한도 초과. {}ms 기다렸다 다시 보낸다", e.retryAfter().toMillis());
                    sleep(e.retryAfter().toMillis());
                } catch (DiscordApiException e) {
                    if (giveUpIfExhausted(notification, e)) {
                        return;
                    }
                    notifications.save(notification);
                    log.warn("알림 발송 실패({}번째). 다음 점검 때 다시 보낸다: {}", notification.getAttempts(), e.getMessage());
                    return;
                }
            }
        }
    }

    private Map<String, Object> buildMessage(Notification notification) {
        Posting posting = postings.findById(notification.getPostingId()).orElseThrow();
        List<MemberFilter> registered = filters.findByIdGuildId(notification.getGuildId());
        List<Long> mentioned = registered.stream()
                .filter(filter -> filter.matches(posting.getTitle(), posting.getCareer()))
                .map(MemberFilter::getUserId)
                .toList();
        // "왜 나는 멘션이 안 됐지" 에 답하려면 조건을 몇 명이 걸어뒀는지까지 있어야 한다. DEBUG 가 아니라 INFO 로 남긴다.
        log.info("멘션 대상 {}명 (조건 등록 {}명): {}", mentioned.size(), registered.size(), posting.getTitle());
        return NotificationMessage.of(posting, mentioned, notification.getTraceId());
    }

    private boolean giveUpIfExhausted(Notification notification, DiscordApiException e) {
        if (notification.getAttempts() < MAX_ATTEMPTS) {
            return false;
        }
        notification.failed();
        notifications.save(notification);
        log.error("알림 발송 포기({}회 실패): {}", notification.getAttempts(), e.getMessage(), e);
        alerter.alert("notification-failed:" + notification.getId(), "알림 발송 포기(서버 " + notification.getGuildId()
                + "). 추적 코드 " + TraceIds.shortCode(notification.getTraceId()) + " — " + e.getMessage());
        return true;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("발송 대기 중 인터럽트", e);
        }
    }
}
