package io.github.shj5771.jobbell.posting.source;

import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.RawPosting;

import java.util.List;

/**
 * 공고 출처 하나. 수집·매칭·발송 코드는 출처를 모른다.
 *
 * <p><b>빈 목록과 실패를 섞지 않는다.</b> 조회에 실패했거나 응답이 기대한 모양이 아니면
 * {@link PostingSourceException} 을 던진다. 실패를 빈 목록으로 돌려주면 "새 공고 0건" 과 구분되지 않아
 * 알림이 끊긴 채로 아무도 모르게 된다.
 */
public interface PostingSource {

    /** {@code company.source_id} 에 저장되는 값. */
    String sourceId();

    /** 지금 열려 있는 공고 전부. 이전 수집과의 차이는 호출한 쪽이 계산한다. */
    List<RawPosting> fetch(Company company);
}
