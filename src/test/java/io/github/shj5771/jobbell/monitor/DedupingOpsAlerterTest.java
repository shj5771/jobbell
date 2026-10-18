package io.github.shj5771.jobbell.monitor;

import io.github.shj5771.jobbell.support.RecordingOpsSink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DedupingOpsAlerterTest {

    private final RecordingOpsSink sink = new RecordingOpsSink();
    private final DedupingOpsAlerter alerter = new DedupingOpsAlerter(sink);

    @Test
    @DisplayName("같은 경보는 풀릴 때까지 한 번만 보내고, 풀리면 복구를 알린다")
    void 중복_거르기() {
        alerter.alert("k", "수집 실패");
        alerter.alert("k", "수집 실패");
        alerter.resolve("k", "수집 복구");
        alerter.resolve("k", "수집 복구");
        alerter.alert("k", "수집 실패");

        assertThat(sink.messages()).containsExactly("⚠ 수집 실패", "✅ 수집 복구", "⚠ 수집 실패");
    }

    @Test
    @DisplayName("경보를 보낸 적 없는 문제의 복구는 알리지 않는다")
    void 열린_적_없는_복구() {
        alerter.resolve("k", "수집 복구");

        assertThat(sink.messages()).isEmpty();
    }

    @Test
    @DisplayName("운영 채널 전송이 실패해도 호출한 쪽(수집·발송)으로 예외가 새지 않는다")
    void 전송_실패는_삼킨다() {
        DedupingOpsAlerter broken = new DedupingOpsAlerter(content -> {
            throw new IllegalStateException("웹훅 404");
        });

        assertThatCode(() -> broken.alert("k", "수집 실패")).doesNotThrowAnyException();
    }
}
