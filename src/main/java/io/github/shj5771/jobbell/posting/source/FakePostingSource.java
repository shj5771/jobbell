package io.github.shj5771.jobbell.posting.source;

import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.RawPosting;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * local 전용 가짜 출처. 외부 API 없이 수집 → 매칭 → 발송 전 구간을 돌려보기 위한 것이다.
 *
 * <p>기업마다 고정 공고 3건이 있고, {@link #NEW_POSTING_EVERY} 마다 새 공고가 1건씩 늘어난다.
 * 조용한 실패를 재현할 수 있게 두 키를 예약해 둔다.
 * <ul>
 *   <li>{@code broken} — 항상 예외를 던진다(출처 장애)</li>
 *   <li>{@code empty} — 항상 빈 목록을 준다(출처 형식이 바뀌어 아무것도 못 읽는 상황)</li>
 * </ul>
 */
@Component
@Profile("local")
public class FakePostingSource implements PostingSource {

    public static final String SOURCE_ID = "fake";

    static final Duration NEW_POSTING_EVERY = Duration.ofMinutes(2);

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String[] BASE_TITLES = {"백엔드 엔지니어 (신입)", "프론트엔드 엔지니어 (경력)", "데이터 엔지니어"};
    private static final String[] BASE_CAREERS = {"신입", "경력", null};

    private final Clock clock;

    public FakePostingSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String sourceId() {
        return SOURCE_ID;
    }

    @Override
    public List<RawPosting> fetch(Company company) {
        String key = company.getSourceKey();
        if ("broken".equals(key)) {
            throw new PostingSourceException("가짜 출처 장애: " + key);
        }
        if ("empty".equals(key)) {
            return List.of();
        }

        LocalDate today = LocalDate.now(clock.withZone(SEOUL));
        List<RawPosting> postings = new ArrayList<>();
        for (int i = 0; i < BASE_TITLES.length; i++) {
            postings.add(new RawPosting(key + "-base-" + i, BASE_TITLES[i], BASE_CAREERS[i],
                    today.plusDays(30), "https://example.com/" + key + "/base-" + i));
        }

        // 시각을 구간으로 나눠 구간마다 공고 ID 하나가 새로 생긴다. 같은 구간 안에서는 몇 번 불러도 같다.
        long bucket = clock.millis() / NEW_POSTING_EVERY.toMillis();
        postings.add(new RawPosting(key + "-" + bucket, "백엔드 엔지니어 #" + bucket, "신입",
                today.plusDays(14), "https://example.com/" + key + "/" + bucket));
        return postings;
    }
}
