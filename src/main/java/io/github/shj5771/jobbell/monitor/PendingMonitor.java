package io.github.shj5771.jobbell.monitor;

import io.github.shj5771.jobbell.notify.Notification;
import io.github.shj5771.jobbell.notify.NotificationRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/** 발송은 비동기라 수집은 성공으로 끝난다. 보내지 못한 채 쌓이는 PENDING 은 따로 봐야 보인다. */
@Component
public class PendingMonitor {

    static final Duration THRESHOLD = Duration.ofMinutes(10);
    private static final String KEY = "pending-stuck";

    private final NotificationRepository notifications;
    private final OpsAlerter alerter;
    private final Clock clock;

    public PendingMonitor(NotificationRepository notifications, OpsAlerter alerter, Clock clock) {
        this.notifications = notifications;
        this.alerter = alerter;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT5M")
    @SchedulerLock(name = "monitor-pending-notifications", lockAtMostFor = "PT2M")
    public void check() {
        int stuck = notifications.findByStatusAndCreatedAtBefore(
                Notification.Status.PENDING, clock.instant().minus(THRESHOLD)).size();
        if (stuck > 0) {
            alerter.alert(KEY, "보내지 못한 알림 " + stuck + "건이 " + THRESHOLD.toMinutes() + "분 넘게 남아 있다");
        } else {
            alerter.resolve(KEY, "밀린 알림 없음");
        }
    }
}
