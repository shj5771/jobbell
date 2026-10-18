package io.github.shj5771.jobbell.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 데코레이터 단독 검증. 같은 스레드를 두 번 써서 "다음 작업이 이전 작업의 잔재를 보는가" 를 본다. */
class MdcTaskDecoratorTest {

    private final TaskDecorator decorator = new AsyncConfig().mdcTaskDecorator();

    @AfterEach
    void clearCallerMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("호출한 쪽의 MDC 가 작업 스레드로 옮겨진다")
    void 호출한_쪽의_MDC가_옮겨진다() throws Exception {
        MDC.put(MdcKeys.TRACE_ID, "abc123");
        MDC.put(MdcKeys.POSTING_ID, "42");

        AtomicReference<Map<String, String>> seenByWorker = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> seenByWorker.set(MDC.getCopyOfContextMap()));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(decorated).get();
        } finally {
            pool.shutdownNow();
        }

        assertThat(seenByWorker.get())
                .containsEntry(MdcKeys.TRACE_ID, "abc123")
                .containsEntry(MdcKeys.POSTING_ID, "42");
    }

    @Test
    @DisplayName("작업이 끝나면 작업 스레드의 MDC 는 실행 전 상태로 되돌아간다")
    void 작업이_끝나면_복구된다() throws Exception {
        MDC.put(MdcKeys.TRACE_ID, "abc123");
        Runnable decorated = decorator.decorate(() -> { });

        AtomicReference<Map<String, String>> leftBehind = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(decorated).get();
            pool.submit(() -> leftBehind.set(MDC.getCopyOfContextMap())).get();
        } finally {
            pool.shutdownNow();
        }

        assertThat(leftBehind.get()).isNullOrEmpty();
    }

    @Test
    @DisplayName("호출한 쪽에 MDC 가 없으면 작업 스레드에 남아 있던 값을 걷어낸다")
    void 호출한_쪽이_비어_있으면_잔재를_걷어낸다() throws Exception {
        Runnable decorated = decorator.decorate(() -> { });

        AtomicReference<Map<String, String>> seenByWorker = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // 데코레이터 밖에서 들어간 잔재를 작업 스레드에 미리 심어둔다
            pool.submit(() -> MDC.put(MdcKeys.TRACE_ID, "STALE")).get();
            pool.submit(decorator.decorate(() -> seenByWorker.set(MDC.getCopyOfContextMap()))).get();
            pool.submit(decorated).get();
        } finally {
            pool.shutdownNow();
        }

        assertThat(seenByWorker.get()).isNullOrEmpty();
    }

    @Test
    @DisplayName("작업이 예외로 끝나도 MDC 는 복구된다")
    void 예외로_끝나도_복구된다() throws Exception {
        MDC.put(MdcKeys.TRACE_ID, "abc123");
        Runnable decorated = decorator.decorate(() -> {
            throw new IllegalStateException("boom");
        });

        AtomicReference<Map<String, String>> leftBehind = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            assertThatThrownBy(() -> pool.submit(decorated).get())
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            pool.submit(() -> leftBehind.set(MDC.getCopyOfContextMap())).get();
        } finally {
            pool.shutdownNow();
        }

        assertThat(leftBehind.get()).isNullOrEmpty();
    }
}
