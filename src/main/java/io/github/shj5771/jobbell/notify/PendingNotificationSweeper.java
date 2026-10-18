package io.github.shj5771.jobbell.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * 남아 있는 PENDING 을 다시 보낸다. 발송 직후 서버가 죽었거나, 일시적 실패로 다음 점검을 기다리는 것들이다.
 *
 * <p>막 만들어진 PENDING 은 지금 비동기로 보내는 중일 수 있으므로 {@link #GRACE} 가 지난 것만 본다.
 */
@Component
public class PendingNotificationSweeper {

    static final Duration GRACE = Duration.ofMinutes(2);

    private static final Logger log = LoggerFactory.getLogger(PendingNotificationSweeper.class);

    private final NotificationRepository notifications;
    private final NotificationSender sender;
    private final Clock clock;

    public PendingNotificationSweeper(NotificationRepository notifications, NotificationSender sender, Clock clock) {
        this.notifications = notifications;
        this.sender = sender;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1M")
    @SchedulerLock(name = "sweep-pending-notifications", lockAtMostFor = "PT5M")
    public void sweep() {
        List<Notification> stale = notifications.findByStatusAndCreatedAtBefore(
                Notification.Status.PENDING, clock.instant().minus(GRACE));
        if (stale.isEmpty()) {
            return;
        }
        log.info("남아 있는 발송 {}건을 다시 보낸다", stale.size());
        stale.forEach(notification -> sender.send(notification.getId()));
    }
}
