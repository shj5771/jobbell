package io.github.shj5771.jobbell.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 중복을 거르고 실제 전송은 {@code sink} 에 맡긴다. 전송 수단과 무관하게 경보는 항상 WARN 로그로도 남긴다 —
 * 운영 채널로 보내는 것 자체가 실패할 수 있기 때문이다.
 *
 * <p>열린 경보는 메모리에만 있다. 재시작하면 같은 문제를 한 번 더 알린다. 그 정도 중복은 받아들인다.
 */
public class DedupingOpsAlerter implements OpsAlerter {

    private static final Logger log = LoggerFactory.getLogger(DedupingOpsAlerter.class);

    private final Set<String> open = ConcurrentHashMap.newKeySet();
    private final Consumer<String> sink;

    public DedupingOpsAlerter(Consumer<String> sink) {
        this.sink = sink;
    }

    @Override
    public void alert(String key, String message) {
        if (!open.add(key)) {
            return;
        }
        log.warn("[운영 경보] {}", message);
        send("⚠ " + message);
    }

    @Override
    public void resolve(String key, String message) {
        if (!open.remove(key)) {
            return;
        }
        log.info("[운영 경보 해제] {}", message);
        send("✅ " + message);
    }

    private void send(String content) {
        try {
            sink.accept(content);
        } catch (RuntimeException e) {
            log.error("운영 채널 전송 실패: {}", e.getMessage(), e);
        }
    }
}
