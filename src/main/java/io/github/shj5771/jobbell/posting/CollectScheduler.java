package io.github.shj5771.jobbell.posting;

import io.github.shj5771.jobbell.monitor.CollectMonitor;
import io.github.shj5771.jobbell.notify.NotificationService;

import java.util.List;
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
    public void collect() {
        List<CollectResult> results = collectService.collectAll();
        collectMonitor.inspect(results);
        notificationService.dispatch(results);
    }
}
