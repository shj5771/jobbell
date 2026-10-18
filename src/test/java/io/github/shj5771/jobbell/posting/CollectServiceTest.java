package io.github.shj5771.jobbell.posting;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.common.MdcKeys;
import io.github.shj5771.jobbell.support.LogCapture;
import io.github.shj5771.jobbell.support.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** local 카탈로그(가짜 출처 4곳: acme, globex, broken, empty)로 수집 전 구간을 돌린다. */
@IntegrationTest
class CollectServiceTest {

    @Autowired
    CollectService collectService;

    @Autowired
    CollectRunRepository collectRuns;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

    LogCapture logs;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM collect_run");
        jdbc.update("DELETE FROM posting");
        logs = LogCapture.attachTo(CollectService.class.getName());
    }

    @AfterEach
    void tearDown() {
        logs.close();
    }

    @Test
    @DisplayName("첫 수집은 기준선이다. 공고는 저장하지만 새 공고로 알릴 대상이 아니라고 표시한다")
    void 첫_수집은_기준선() {
        Map<String, CollectResult> results = byKey(collectService.collectAll());

        assertThat(results.get("acme").succeeded()).isTrue();
        assertThat(results.get("acme").baseline()).isTrue();
        assertThat(results.get("acme").newPostings()).hasSize(4);
    }

    @Test
    @DisplayName("같은 구간에 다시 돌면 새 공고가 없고, 시간이 지나면 한 건 생긴다")
    void 이미_본_공고는_다시_저장하지_않는다() {
        collectService.collectAll();

        assertThat(byKey(collectService.collectAll()).get("acme").newPostings()).isEmpty();

        clock.advance(Duration.ofMinutes(2));
        CollectResult later = byKey(collectService.collectAll()).get("acme");
        assertThat(later.baseline()).isFalse();
        assertThat(later.newPostings()).hasSize(1);
    }

    @Test
    @DisplayName("한 기업의 실패가 다른 기업 수집을 막지 않고, 실패는 기록으로 남는다")
    void 실패는_기록되고_나머지는_계속된다() {
        Map<String, CollectResult> results = byKey(collectService.collectAll());

        assertThat(results.get("broken").succeeded()).isFalse();
        assertThat(results.get("globex").succeeded()).isTrue();

        CollectRun brokenRun = collectRuns.findAll().stream()
                .filter(run -> run.getCompanyId().equals(results.get("broken").company().getId()))
                .findFirst().orElseThrow();
        assertThat(brokenRun.getStatus()).isEqualTo(CollectRun.Status.FAILED);
        assertThat(brokenRun.getError()).contains("broken");
    }

    @Test
    @DisplayName("빈 응답은 실패가 아니라 '열린 공고 0건' 으로 기록된다")
    void 빈_응답은_0건_성공() {
        CollectResult empty = byKey(collectService.collectAll()).get("empty");

        assertThat(empty.succeeded()).isTrue();
        CollectRun run = collectRuns.findTopByCompanyIdAndStatusOrderByStartedAtDesc(
                empty.company().getId(), CollectRun.Status.SUCCESS).orElseThrow();
        assertThat(run.getFetchedCount()).isZero();
    }

    @Test
    @DisplayName("모든 수집 로그에 runId·traceId, 기업 단위 로그에 traceId·companyId, 새 공고 로그에 postingId 가 붙는다")
    void 로그에_추적_키가_붙는다() {
        collectService.collectAll();
        List<ILoggingEvent> events = logs.events();

        assertThat(events).allSatisfy(e -> assertThat(e.getMDCPropertyMap()).containsKeys(MdcKeys.RUN_ID, MdcKeys.TRACE_ID));
        assertThat(events).filteredOn(e -> e.getFormattedMessage().startsWith("수집 성공")
                        || e.getFormattedMessage().startsWith("수집 실패"))
                .hasSize(4)
                .allSatisfy(e -> assertThat(e.getMDCPropertyMap()).containsKeys(MdcKeys.TRACE_ID, MdcKeys.COMPANY_ID));
        assertThat(events).filteredOn(e -> e.getFormattedMessage().startsWith("새 공고"))
                .isNotEmpty()
                .allSatisfy(e -> assertThat(e.getMDCPropertyMap()).containsKey(MdcKeys.POSTING_ID));
    }

    @Test
    @DisplayName("기업마다 traceId 가 다르고, 수집이 끝나면 스레드에 MDC 가 남지 않는다")
    void 기업마다_traceId가_다르고_정리된다() {
        List<CollectResult> results = collectService.collectAll();

        assertThat(results).extracting(CollectResult::traceId).doesNotHaveDuplicates();
        assertThat(org.slf4j.MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private static Map<String, CollectResult> byKey(List<CollectResult> results) {
        return results.stream().collect(Collectors.toMap(r -> r.company().getSourceKey(), Function.identity()));
    }
}
