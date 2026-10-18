package io.github.shj5771.jobbell.notify;

import io.github.shj5771.jobbell.common.MdcKeys;
import io.github.shj5771.jobbell.common.MdcScope;
import io.github.shj5771.jobbell.guild.Guild;
import io.github.shj5771.jobbell.guild.GuildCompany;
import io.github.shj5771.jobbell.guild.GuildCompanyRepository;
import io.github.shj5771.jobbell.guild.GuildRepository;
import io.github.shj5771.jobbell.posting.CollectResult;
import io.github.shj5771.jobbell.posting.Posting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * 수집 결과에서 "어느 서버에 어느 공고를 보낼지" 를 정하고 발송을 넘긴다.
 *
 * <p>알리지 않는 경우와 이유를 전부 INFO 로 남긴다. "왜 알림이 안 왔지" 의 답은 대부분 여기 있다.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final GuildCompanyRepository guildCompanies;
    private final GuildRepository guilds;
    private final NotificationRepository notifications;
    private final NotificationSender sender;
    private final Clock clock;

    public NotificationService(GuildCompanyRepository guildCompanies, GuildRepository guilds,
                               NotificationRepository notifications, NotificationSender sender, Clock clock) {
        this.guildCompanies = guildCompanies;
        this.guilds = guilds;
        this.notifications = notifications;
        this.sender = sender;
        this.clock = clock;
    }

    public void dispatch(List<CollectResult> results) {
        for (CollectResult result : results) {
            if (!result.succeeded() || result.baseline() || result.newPostings().isEmpty()) {
                continue;
            }
            // 수집의 traceId 를 다시 연다. 여기서 넘긴 비동기 발송이 이 값을 물려받는다.
            try (MdcScope ignored = MdcScope.open()) {
                MDC.put(MdcKeys.TRACE_ID, result.traceId());
                MDC.put(MdcKeys.COMPANY_ID, String.valueOf(result.company().getId()));
                List<GuildCompany> subscribers = guildCompanies.findByIdCompanyId(result.company().getId());
                for (Posting posting : result.newPostings()) {
                    MDC.put(MdcKeys.POSTING_ID, String.valueOf(posting.getId()));
                    plan(posting, subscribers, result.traceId());
                }
            }
        }
    }

    private void plan(Posting posting, List<GuildCompany> subscribers, String traceId) {
        if (posting.isClosedOn(LocalDate.now(clock.withZone(SEOUL)))) {
            log.info("알리지 않음: 마감이 지난 공고 ({})", posting.getDeadline());
            return;
        }
        if (subscribers.isEmpty()) {
            log.info("알리지 않음: 이 기업을 관심 기업으로 둔 서버가 없다");
            return;
        }
        for (GuildCompany subscriber : subscribers) {
            MDC.put(MdcKeys.GUILD_ID, String.valueOf(subscriber.getGuildId()));
            Optional<Long> channel = guilds.findById(subscriber.getGuildId()).map(Guild::getAlertChannelId);
            if (channel.isEmpty()) {
                log.info("알리지 않음: 알림 채널이 정해지지 않은 서버 (/채널설정 전)");
            } else if (!subscriber.getAddedAt().isBefore(posting.getFirstSeenAt())) {
                log.info("알리지 않음: 관심 기업 추가({}) 전에 이미 보인 공고", subscriber.getAddedAt());
            } else {
                createPending(subscriber.getGuildId(), posting.getId(), traceId)
                        .ifPresent(sender::sendAsync);
            }
        }
        MDC.remove(MdcKeys.GUILD_ID);
    }

    /** 이미 있으면 비운다. 존재 확인과 insert 사이에 다른 인스턴스가 끼어들면 유니크 제약이 마지막으로 막는다. */
    private Optional<Long> createPending(long guildId, long postingId, String traceId) {
        if (notifications.existsByGuildIdAndPostingId(guildId, postingId)) {
            log.info("알리지 않음: 이미 발송 기록이 있다");
            return Optional.empty();
        }
        try {
            return Optional.of(notifications.save(new Notification(guildId, postingId, traceId, clock.instant())).getId());
        } catch (DataIntegrityViolationException e) {
            log.info("알리지 않음: 같은 발송이 동시에 만들어졌다(유니크 제약)");
            return Optional.empty();
        }
    }
}
