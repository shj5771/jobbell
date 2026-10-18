package io.github.shj5771.jobbell.posting;

import java.util.List;

/**
 * 기업 한 곳 수집 결과.
 *
 * @param baseline 이 기업의 첫 성공 수집이면 true. 그때 보인 공고는 "새 공고" 가 아니라 기준선이라 알리지 않는다
 * @param traceId  이 수집의 traceId. 이어지는 발송이 물려받는다
 */
public record CollectResult(Company company, boolean succeeded, boolean baseline, List<Posting> newPostings,
                            String traceId) {

    public static CollectResult failed(Company company, String traceId) {
        return new CollectResult(company, false, false, List.of(), traceId);
    }
}
