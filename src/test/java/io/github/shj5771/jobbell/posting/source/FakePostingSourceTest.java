package io.github.shj5771.jobbell.posting.source;

import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.RawPosting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakePostingSourceTest {

    private static final Instant T0 = Instant.parse("2026-10-18T00:00:00Z");

    @Test
    @DisplayName("같은 구간 안에서는 몇 번 불러도 같은 공고다")
    void 같은_구간은_같은_공고() {
        FakePostingSource source = at(T0);

        assertThat(source.fetch(company("acme"))).isEqualTo(source.fetch(company("acme")));
    }

    @Test
    @DisplayName("구간이 지나면 새 공고가 하나 생긴다")
    void 구간이_지나면_새_공고() {
        List<String> before = ids(at(T0).fetch(company("acme")));
        List<String> after = ids(at(T0.plus(FakePostingSource.NEW_POSTING_EVERY)).fetch(company("acme")));

        assertThat(after).hasSize(before.size());
        assertThat(after).filteredOn(id -> !before.contains(id)).hasSize(1);
    }

    @Test
    @DisplayName("broken 은 예외, empty 는 빈 목록 — 조용한 실패 재현용")
    void 예약된_키() {
        assertThatThrownBy(() -> at(T0).fetch(company("broken"))).isInstanceOf(PostingSourceException.class);
        assertThat(at(T0).fetch(company("empty"))).isEmpty();
    }

    private static FakePostingSource at(Instant instant) {
        return new FakePostingSource(Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static Company company(String key) {
        return new Company(key, FakePostingSource.SOURCE_ID, key);
    }

    private static List<String> ids(List<RawPosting> postings) {
        return postings.stream().map(RawPosting::externalId).toList();
    }
}
