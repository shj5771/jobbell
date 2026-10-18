package io.github.shj5771.jobbell.posting;

import io.github.shj5771.jobbell.common.MdcKeys;
import io.github.shj5771.jobbell.common.MdcScope;
import io.github.shj5771.jobbell.common.TraceIds;
import io.github.shj5771.jobbell.posting.source.PostingSource;
import io.github.shj5771.jobbell.posting.source.PostingSourceException;
import io.github.shj5771.jobbell.posting.source.PostingSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 카탈로그 기업의 공고를 가져와 처음 보는 것만 저장한다.
 *
 * <p>여기는 HTTP 요청이 아니라 스케줄러에서 시작된다. 필터가 없으므로 traceId 를 이 클래스가 직접 발급한다.
 * <ul>
 *   <li>수집 1회 → {@code runId}. 한 번의 실행이 여러 기업을 돌므로 그 묶음을 가리킨다.</li>
 *   <li>기업 1곳 → 새 {@code traceId}. 이어지는 발송이 이 값을 물려받는다.</li>
 * </ul>
 * 두 스코프 모두 {@link MdcScope} 로 열고 닫는다. 스케줄러 스레드도 재사용되기 때문이다.
 */
@Service
public class CollectService {

    private static final Logger log = LoggerFactory.getLogger(CollectService.class);

    private final CatalogLoader catalog;
    private final PostingSources sources;
    private final PostingRepository postings;
    private final CollectRunRepository collectRuns;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public CollectService(CatalogLoader catalog, PostingSources sources, PostingRepository postings,
                          CollectRunRepository collectRuns, TransactionTemplate transaction, Clock clock) {
        this.catalog = catalog;
        this.sources = sources;
        this.postings = postings;
        this.collectRuns = collectRuns;
        this.transaction = transaction;
        this.clock = clock;
    }

    public List<CollectResult> collectAll() {
        try (MdcScope ignored = MdcScope.open()) {
            String runId = TraceIds.newId();
            MDC.put(MdcKeys.RUN_ID, runId);
            // 실행 단위 줄("수집 시작/끝")에도 traceId 를 채운다. NO_TRACE 는 "추적이 끊겼다" 는 신호로만 남겨둔다.
            MDC.put(MdcKeys.TRACE_ID, runId);

            List<Company> companies = catalog.catalogCompanies();
            log.info("수집 시작: 기업 {}곳", companies.size());

            List<CollectResult> results = new ArrayList<>();
            for (Company company : companies) {
                results.add(collectOne(runId, company));
            }

            long failed = results.stream().filter(r -> !r.succeeded()).count();
            int created = results.stream().mapToInt(r -> r.newPostings().size()).sum();
            log.info("수집 끝: 성공 {}곳, 실패 {}곳, 새 공고 {}건", results.size() - failed, failed, created);
            return results;
        }
    }

    private CollectResult collectOne(String runId, Company company) {
        try (MdcScope ignored = MdcScope.open()) {
            String traceId = TraceIds.newId();
            MDC.put(MdcKeys.TRACE_ID, traceId);
            MDC.put(MdcKeys.SOURCE_ID, company.getSourceId());
            MDC.put(MdcKeys.COMPANY_ID, String.valueOf(company.getId()));

            CollectRun run = collectRuns.save(CollectRun.start(runId, company, clock.instant()));
            boolean baseline = !collectRuns.existsByCompanyIdAndStatus(company.getId(), CollectRun.Status.SUCCESS);

            List<RawPosting> fetched;
            try {
                PostingSource source = sources.find(company.getSourceId())
                        .orElseThrow(() -> new PostingSourceException("등록되지 않은 출처: " + company.getSourceId()));
                fetched = source.fetch(company);
            } catch (RuntimeException e) {
                // PostingSourceException 이 아닌 예외(파싱 버그 등)도 여기서 잡는다. 한 기업의 실패가 나머지 수집을 멈추면 안 된다.
                run.fail(e.getMessage(), clock.instant());
                collectRuns.save(run);
                log.error("수집 실패: {} — {}", company.getName(), e.getMessage(), e);
                return CollectResult.failed(company, traceId);
            }

            List<Posting> created = transaction.execute(status -> saveNew(company, fetched));
            run.succeed(fetched.size(), created.size(), clock.instant());
            collectRuns.save(run);

            log.info("수집 성공: {} — 열린 공고 {}건, 새 공고 {}건{}", company.getName(), fetched.size(), created.size(),
                    baseline ? " (첫 수집이라 알리지 않고 기준선으로만 기록)" : "");
            for (Posting posting : created) {
                MDC.put(MdcKeys.POSTING_ID, String.valueOf(posting.getId()));
                log.info("새 공고: {} [{}]", posting.getTitle(), posting.getExternalId());
            }
            MDC.remove(MdcKeys.POSTING_ID);

            return new CollectResult(company, true, baseline, created, traceId);
        }
    }

    private List<Posting> saveNew(Company company, List<RawPosting> fetched) {
        List<Posting> created = new ArrayList<>();
        for (RawPosting raw : fetched) {
            if (!postings.existsBySourceIdAndExternalId(company.getSourceId(), raw.externalId())) {
                created.add(postings.save(new Posting(company, raw, clock.instant())));
            }
        }
        return created;
    }
}
