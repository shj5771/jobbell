package io.github.shj5771.jobbell.posting;

import io.github.shj5771.jobbell.monitor.CollectMonitor;
import io.github.shj5771.jobbell.notify.NotificationService;

import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectScheduler {

    private final CollectService collectService;
    private final CollectMonitor collectMonitor;
    private final NotificationService notificationService;

    public CollectScheduler(CollectService collectService, CollectMonitor collectMonitor,
                            NotificationService notificationService) {
        this.collectService = collectService;
        this.collectMonitor = collectMonitor;
        this.notificationService = notificationService;
    }

    /** fixedDelay: 앞 수집이 끝난 뒤부터 잰다. 수집이 길어져도 두 번이 겹치지 않는다. */
    @Scheduled(initialDelayString = "${app.collect.initial-delay}", fixedDelayString = "${app.collect.interval}")
    // lockAtLeastFor: 다른 인스턴스가 방금 끝낸 수집을 곧바로 다시 돌지 않게 한다.
    @SchedulerLock(name = "collect", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void collect() {
        List<CollectResult> results = collectService.collectAll();
        collectMonitor.inspect(results);
        notificationService.dispatch(results);
    }
}
