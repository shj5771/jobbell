package io.github.shj5771.jobbell.posting;

import java.time.LocalDate;

/**
 * 출처가 돌려준 공고 한 건. 출처마다 다른 응답을 이 모양으로 맞춰 넘긴다.
 *
 * @param externalId 출처 안에서의 공고 ID. 새 공고 판단의 기준이다
 * @param career     출처가 주지 않으면 null
 * @param deadline   출처가 주지 않으면 null
 */
public record RawPosting(String externalId, String title, String career, LocalDate deadline, String url) {
}
