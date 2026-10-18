package io.github.shj5771.jobbell.posting;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectScheduler {

    private final CollectService collectService;

    public CollectScheduler(CollectService collectService) {
        this.collectService = collectService;
    }

    /** fixedDelay: 앞 수집이 끝난 뒤부터 잰다. 수집이 길어져도 두 번이 겹치지 않는다. */
    @Scheduled(initialDelayString = "${app.collect.initial-delay}", fixedDelayString = "${app.collect.interval}")
    public void collect() {
        collectService.collectAll();
    }
}
