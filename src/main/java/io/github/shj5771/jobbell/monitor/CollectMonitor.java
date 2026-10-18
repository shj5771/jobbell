package io.github.shj5771.jobbell.monitor;

import io.github.shj5771.jobbell.common.TraceIds;
import io.github.shj5771.jobbell.posting.CollectResult;
import io.github.shj5771.jobbell.posting.CollectRun;
import io.github.shj5771.jobbell.posting.CollectRunRepository;
import io.github.shj5771.jobbell.posting.Company;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 수집 결과에서 예외가 안 나는 실패를 찾는다.
 *
 * <p>가장 위험한 것은 출처 응답 형식이 바뀌어 파싱 결과가 0건이 되는 경우다. 수집은 "성공" 이고
 * "새 공고 없음" 과 구분되지 않는다. 그래서 새 공고 수가 아니라 <b>열린 공고 수</b>가 양수에서 0 으로
 * 떨어지는 것을 본다. 채용을 통째로 닫은 기업이면 오탐이지만, 그건 사람이 한 번 보면 되는 일이다.
 */
@Component
public class CollectMonitor {

    private final CollectRunRepository collectRuns;
    private final OpsAlerter alerter;

    public CollectMonitor(CollectRunRepository collectRuns, OpsAlerter alerter) {
        this.collectRuns = collectRuns;
        this.alerter = alerter;
    }

    public void inspect(List<CollectResult> results) {
        for (CollectResult result : results) {
            Company company = result.company();
            String failureKey = "collect-failed:" + company.getId();
            String emptyKey = "collect-empty:" + company.getId();

            if (!result.succeeded()) {
                alerter.alert(failureKey, company.getName() + " 수집 실패. 추적 코드 " + TraceIds.shortCode(result.traceId()));
                continue;
            }
            alerter.resolve(failureKey, company.getName() + " 수집 복구");

            List<CollectRun> lastTwo =
                    collectRuns.findTop2ByCompanyIdAndStatusOrderByStartedAtDesc(company.getId(), CollectRun.Status.SUCCESS);
            if (lastTwo.size() < 2) {
                continue;
            }
            int now = lastTwo.get(0).getFetchedCount();
            int before = lastTwo.get(1).getFetchedCount();
            if (now == 0 && before > 0) {
                alerter.alert(emptyKey, company.getName() + " 열린 공고가 " + before + "건 → 0건. 출처 응답 형식이 바뀌었는지 확인할 것");
            } else if (now > 0) {
                alerter.resolve(emptyKey, company.getName() + " 열린 공고 다시 " + now + "건");
            }
        }
    }
}
