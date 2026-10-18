package io.github.shj5771.jobbell.monitor;

import io.github.shj5771.jobbell.posting.CatalogLoader;
import io.github.shj5771.jobbell.posting.CollectRun;
import io.github.shj5771.jobbell.posting.CollectRunRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 스케줄러가 멈추면 로그가 안 나오는 것 자체가 증상이라 앱 안에서는 알아챌 수 없다.
 * 마지막 수집 성공이 {@link #STALE_AFTER} 넘게 지나면 헬스체크를 DOWN 으로 돌리고,
 * 서버 밖의 외부 감시가 이 값을 본다. 서버가 통째로 죽어도 잡히는 유일한 경로다.
 */
@Component("collect")
public class CollectHealthIndicator implements HealthIndicator {

    static final Duration STALE_AFTER = Duration.ofMinutes(30);

    private final CollectRunRepository collectRuns;
    private final CatalogLoader catalog;
    private final Clock clock;
    private final Instant startedAt;

    public CollectHealthIndicator(CollectRunRepository collectRuns, CatalogLoader catalog, Clock clock) {
        this.collectRuns = collectRuns;
        this.catalog = catalog;
        this.clock = clock;
        this.startedAt = clock.instant();
    }

    @Override
    public Health health() {
        if (catalog.catalogCompanies().isEmpty()) {
            return Health.up().withDetail("reason", "카탈로그가 비어 있다").build();
        }
        Instant now = clock.instant();
        Optional<Instant> lastSuccess = collectRuns.findTopByStatusOrderByFinishedAtDesc(CollectRun.Status.SUCCESS)
                .map(CollectRun::getFinishedAt);
        // 막 뜬 직후에는 아직 수집 전일 수 있다. 기동 시각도 같은 기준으로 본다.
        Instant reference = lastSuccess.filter(at -> at.isAfter(startedAt)).orElse(startedAt);
        Duration since = Duration.between(reference, now);
        Health.Builder health = since.compareTo(STALE_AFTER) > 0 ? Health.down() : Health.up();
        return health.withDetail("lastSuccess", lastSuccess.map(Instant::toString).orElse("없음"))
                .withDetail("sinceMinutes", since.toMinutes())
                .build();
    }
}
