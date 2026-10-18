package io.github.shj5771.jobbell.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 데코레이터가 아무리 정확해도 {@code @Async} 실행기에 붙지 않으면 아무 일도 일어나지 않는다.
 * 그 실패는 예외도 경고도 없이 조용하므로 배선을 따로 고정한다.
 */
class AsyncConfigWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
            .withUserConfiguration(AsyncConfig.class);

    @Test
    @DisplayName("TaskDecorator 빈이 하나 더 생겨도 실행기에는 MDC 데코레이터가 걸려 있다")
    void 데코레이터가_둘이어도_MDC_데코레이터가_걸린다() {
        runner.withBean("otherDecorator", TaskDecorator.class, () -> runnable -> runnable)
                .run(context -> {
                    ThreadPoolTaskExecutor executor =
                            context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class);

                    assertThat(ReflectionTestUtils.getField(executor, "taskDecorator"))
                            .isSameAs(context.getBean("mdcTaskDecorator"));
                });
    }
}
