package io.github.shj5771.jobbell.monitor;

import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.notify.Notification;
import io.github.shj5771.jobbell.notify.NotificationRepository;
import io.github.shj5771.jobbell.posting.CatalogLoader;
import io.github.shj5771.jobbell.posting.CollectResult;
import io.github.shj5771.jobbell.posting.CollectRun;
import io.github.shj5771.jobbell.posting.CollectRunRepository;
import io.github.shj5771.jobbell.posting.CollectService;
import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.support.Database;
import io.github.shj5771.jobbell.support.MutableClock;
import io.github.shj5771.jobbell.support.RecordingOpsSink;
import io.github.shj5771.jobbell.support.ResettableOpsAlerter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class MonitorFlowTest {

    @Autowired
    CollectService collectService;

    @Autowired
    CollectMonitor collectMonitor;

    @Autowired
    PendingMonitor pendingMonitor;

    @Autowired
    CollectHealthIndicator health;

    @Autowired
    CollectRunRepository collectRuns;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    CatalogLoader catalog;

    @Autowired
    RecordingOpsSink ops;

    @Autowired
    ResettableOpsAlerter alerter;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Database.clean(jdbc);
        alerter.reset();
    }

    @Test
    @DisplayName("수집 실패는 한 번만 경보하고, 다음 수집도 실패면 조용하다")
    void 수집_실패_경보() {
        collectMonitor.inspect(collectService.collectAll());
        collectMonitor.inspect(collectService.collectAll());

        assertThat(ops.messages()).filteredOn(m -> m.contains("장애상사 수집 실패")).hasSize(1);
    }

    @Test
    @DisplayName("열린 공고 수가 양수에서 0 으로 떨어지면 경보한다 — 형식 변경이 '새 공고 없음' 에 묻히지 않게")
    void 열린_공고_급락_경보() {
        Company acme = company("acme");
        CollectRun before = CollectRun.start("run-1", acme, clock.instant());
        before.succeed(5, 0, clock.instant());
        collectRuns.save(before);
        clock.advance(Duration.ofMinutes(10));
        CollectRun now = CollectRun.start("run-2", acme, clock.instant());
        now.succeed(0, 0, clock.instant());
        collectRuns.save(now);

        collectMonitor.inspect(List.of(new CollectResult(acme, true, false, List.of(), "a".repeat(32))));

        assertThat(ops.messages()).singleElement().asString().contains("가짜전자 열린 공고가 5건 → 0건");
    }

    @Test
    @DisplayName("처음부터 0건인 기업은 경보하지 않는다 — 떨어진 게 아니다")
    void 처음부터_0건() {
        collectMonitor.inspect(collectService.collectAll());
        clock.advance(Duration.ofMinutes(10));
        collectMonitor.inspect(collectService.collectAll());

        assertThat(ops.messages()).noneMatch(m -> m.contains("빈응답상사"));
    }

    @Test
    @DisplayName("마지막 수집 성공이 30분 넘게 지나면 헬스체크가 DOWN 이고 /actuator/health 가 503 이다")
    void 스케줄러_멈춤() throws Exception {
        collectService.collectAll();
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);

        clock.advance(CollectHealthIndicator.STALE_AFTER.plusMinutes(1));
        assertThat(health.health().getStatus()).isEqualTo(Status.DOWN);
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components").doesNotExist());

        collectService.collectAll();
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("10분 넘게 남은 PENDING 이 있으면 경보한다")
    void 밀린_발송() {
        jdbc.update("INSERT INTO guild (guild_id, created_at) VALUES (1, NOW(6))");
        collectService.collectAll();
        long postingId = jdbc.queryForObject("SELECT MIN(id) FROM posting", Long.class);
        notifications.save(new Notification(1L, postingId, "b".repeat(32), clock.instant()));

        pendingMonitor.check();
        assertThat(ops.messages()).isEmpty();

        clock.advance(PendingMonitor.THRESHOLD.plusMinutes(1));
        pendingMonitor.check();
        assertThat(ops.messages()).singleElement().asString().contains("보내지 못한 알림 1건");
    }

    private Company company(String key) {
        return catalog.catalogCompanies().stream().filter(c -> c.getSourceKey().equals(key)).findFirst().orElseThrow();
    }
}
