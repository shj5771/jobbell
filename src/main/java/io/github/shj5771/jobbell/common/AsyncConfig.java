package io.github.shj5771.jobbell.common;

import org.slf4j.MDC;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * {@code @Async} 작업을 다른 스레드로 넘길 때 호출한 쪽의 MDC 를 함께 옮긴다.
     *
     * <p>{@code decorate()} 는 작업을 "넘기는 쪽"에서 실행되고,
     * 반환한 {@code Runnable} 의 {@code run()} 은 "받는 쪽"(작업 스레드)에서 실행된다.
     * 그래서 컨텍스트 복사는 decorate 시점에, 이식과 복구는 run 시점에 일어난다.
     *
     * <p>받는 쪽도 풀 스레드라 재사용된다. {@link MdcScope} 로 감싸 실행 전 상태로 되돌린다 —
     * 작업이 예외로 끝나도 마찬가지다.
     */
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> callerContext = MDC.getCopyOfContextMap();
            return () -> {
                try (MdcScope ignored = MdcScope.openWith(callerContext)) {
                    runnable.run();
                }
            };
        };
    }

    /**
     * {@code @Async} 가 쓰는 실행기를 직접 정의하고 데코레이터를 <b>명시적으로</b> 건다.
     *
     * <p>이 빈이 없어도 Boot 자동설정이 {@code ObjectProvider<TaskDecorator>.getIfUnique()} 로
     * 위 빈을 주워간다. 문제는 {@code getIfUnique()} 가 <b>후보가 둘 이상이면 null</b> 을 준다는 점이다.
     * 다른 용도의 데코레이터를 하나 더 추가하는 순간 MDC 전파가 통째로 빠지는데, 기동 실패도 경고도 없다.
     *
     * <p>적용 범위는 이 실행기뿐이다. 직접 만든 스레드풀이나 {@code CompletableFuture} 의
     * 기본 ForkJoinPool 로 넘긴 작업에는 전파되지 않는다.
     */
    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public ThreadPoolTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        return builder.taskDecorator(mdcTaskDecorator()).build();
    }
}
