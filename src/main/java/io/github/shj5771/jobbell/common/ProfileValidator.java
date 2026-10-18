package io.github.shj5771.jobbell.common;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 활성 프로파일이 아는 이름인지 기동 시 검사하고, 아니면 기동을 실패시킨다.
 *
 * <p>{@code spring.profiles.default} 는 프로파일을 <b>하나도</b> 주지 않은 경우만 막는다.
 * {@code prodd} 처럼 <b>틀리게</b> 준 경우는 경고도 예외도 없이 정상 기동한다.
 * 그러면 {@code application-prod.yml} 도 {@code <springProfile>} 블록도 적용되지 않은 채
 * local 도 dev 도 prod 도 아닌 네 번째 상태로 돈다.
 */
@Component
public class ProfileValidator {

    private static final Set<String> ALLOWED = Set.of("local", "dev", "prod");

    private final Environment environment;

    public ProfileValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * 프로파일을 하나도 주지 않은 실행은 통과시킨다.
     * 그 경우 활성 프로파일은 빈 배열이고, application.yml 의 {@code spring.profiles.default} 가 맡는다.
     * 여기서 막으면 인자 없는 {@code ./gradlew bootRun} 이 기동에 실패한다.
     */
    @PostConstruct
    void validate() {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            return;
        }

        List<String> unknown = Arrays.stream(active)
                .filter(profile -> !ALLOWED.contains(profile))
                .toList();

        if (!unknown.isEmpty()) {
            throw new IllegalStateException(
                    "알 수 없는 프로파일: " + unknown + ". 허용: " + ALLOWED
                            + ". 오타로 뜨면 그 프로파일의 설정이 조용히 빠진다.");
        }
    }
}
