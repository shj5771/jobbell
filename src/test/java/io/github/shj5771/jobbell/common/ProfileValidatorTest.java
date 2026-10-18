package io.github.shj5771.jobbell.common;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 여기서 검증하는 것은 "예외를 던지는가" 가 아니라 <b>기동이 실패하는가</b> 다.
 * 그래서 {@code validate()} 를 직접 부르지 않고 컨텍스트를 실제로 띄운다 —
 * {@code @Component} 나 {@code @PostConstruct} 가 빠지면 검사 자체가 실행되지 않는데,
 * 메서드를 직접 부르는 테스트는 그 사고를 통과시킨다.
 *
 * <p>{@code @SpringBootTest} 로 프로파일을 바꿔가며 띄우면 JVM 전역인 Logback {@code LoggerContext}
 * 가 캐시돼 프로파일별 설정이 다시 읽히지 않는다. {@code ApplicationContextRunner} 는 그 문제를 피한다.
 */
class ProfileValidatorTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ProfileValidator.class);

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"local", "dev", "prod"})
    @DisplayName("아는 프로파일은 통과한다")
    void 아는_프로파일은_통과한다(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile)
                .run(context -> assertThat(context).hasNotFailed());
    }

    /** {@code spring.profiles.default} 는 active 가 아니다. 활성 프로파일이 빈 배열이어도 통과해야 한다. */
    @Test
    @DisplayName("프로파일을 하나도 주지 않은 실행은 통과한다")
    void 프로파일을_주지_않으면_통과한다() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("오타는 조용히 넘어가지 않고 기동을 실패시킨다")
    void 오타는_기동을_실패시킨다() {
        runner.withPropertyValues("spring.profiles.active=prodd")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("prodd"));
    }

    /** 무엇으로 고쳐야 하는지 알려주지 않으면 시끄러운 실패의 절반만 한 것이다. 메시지도 계약이다. */
    @Test
    @DisplayName("실패 메시지가 허용 목록을 알려준다")
    void 실패_메시지가_허용_목록을_알려준다() {
        runner.withPropertyValues("spring.profiles.active=prodd")
                .run(context -> assertThat(context)
                        .getFailure()
                        .rootCause()
                        .hasMessageContainingAll("local", "dev", "prod"));
    }

    @Test
    @DisplayName("여러 프로파일 중 하나만 틀려도 실패한다")
    void 여러_프로파일_중_하나만_틀려도_실패한다() {
        runner.withPropertyValues("spring.profiles.active=dev,prodd")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("prodd"));
    }

    /**
     * 위 테스트들은 {@code withUserConfiguration} 이 클래스를 직접 등록하므로 {@code @Component} 가 없어도 통과한다.
     * 실제 앱에서 검사가 도는 조건(스캔 + 생명주기 훅)은 따로 고정한다.
     */
    @Test
    @DisplayName("컴포넌트 스캔과 생명주기 훅에 걸리는 형태여야 한다")
    void 스캔과_생명주기_훅에_걸린다() throws NoSuchMethodException {
        assertThat(ProfileValidator.class.isAnnotationPresent(Component.class))
                .as("@Component 가 빠지면 스캔되지 않아 검사가 아예 실행되지 않는다")
                .isTrue();

        Method validate = ProfileValidator.class.getDeclaredMethod("validate");
        assertThat(validate.isAnnotationPresent(PostConstruct.class))
                .as("@PostConstruct 가 빠지면 아무도 validate() 를 부르지 않는다")
                .isTrue();
    }
}
