package io.github.shj5771.jobbell.support;

import io.github.shj5771.jobbell.monitor.DedupingOpsAlerter;
import io.github.shj5771.jobbell.monitor.OpsAlerter;

/**
 * 통합 테스트는 경보기 하나를 공유한다. 앞 테스트가 열어둔 경보가 다음 테스트의 경보를 중복으로 걸러내지 않게,
 * 테스트마다 실제 구현을 새로 만든다. 운영 채널로 나갈 내용은 {@link RecordingOpsSink} 에 모인다.
 */
public class ResettableOpsAlerter implements OpsAlerter {

    private final RecordingOpsSink sink;
    private volatile DedupingOpsAlerter delegate;

    public ResettableOpsAlerter(RecordingOpsSink sink) {
        this.sink = sink;
        reset();
    }

    public void reset() {
        sink.reset();
        delegate = new DedupingOpsAlerter(sink);
    }

    @Override
    public void alert(String key, String message) {
        delegate.alert(key, message);
    }

    @Override
    public void resolve(String key, String message) {
        delegate.resolve(key, message);
    }
}
