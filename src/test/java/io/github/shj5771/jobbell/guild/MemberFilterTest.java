package io.github.shj5771.jobbell.guild;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberFilterTest {

    @Test
    @DisplayName("키워드는 대소문자와 앞뒤 공백을 가리지 않는다")
    void 키워드() {
        MemberFilter filter = new MemberFilter(1L, 2L, "  Backend ", null);

        assertThat(filter.matches("Senior backend Engineer", null)).isTrue();
        assertThat(filter.matches("Frontend Engineer", null)).isFalse();
    }

    @Test
    @DisplayName("경력 조건이 있으면 공고 경력과 맞아야 한다. 공고 경력을 모르거나 '무관' 이면 맞는 것으로 본다")
    void 경력() {
        MemberFilter filter = new MemberFilter(1L, 2L, "백엔드", "신입");

        assertThat(filter.matches("백엔드 엔지니어", "신입")).isTrue();
        assertThat(filter.matches("백엔드 엔지니어", "경력 3년 이상")).isFalse();
        assertThat(filter.matches("백엔드 엔지니어", null)).isTrue();
        assertThat(filter.matches("백엔드 엔지니어", "경력 무관")).isTrue();
    }
}
