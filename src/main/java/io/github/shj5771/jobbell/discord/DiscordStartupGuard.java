package io.github.shj5771.jobbell.discord;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 디스코드 설정이 프로파일과 어긋나면 기동을 막는다.
 *
 * <ul>
 *   <li>서명 검증을 끈 채로 local 밖에서 뜨면 아무나 명령을 위조할 수 있다.</li>
 *   <li>prod 가 아닌데 운영 봇 ID 로 뜨면 개발 중인 코드가 스터디원에게 실제 알림을 보낸다.
 *       프로파일만 dev 로 주고 토큰은 운영 것을 넣은 경우다.</li>
 * </ul>
 */
@Component
public class DiscordStartupGuard {

    private static final Logger log = LoggerFactory.getLogger(DiscordStartupGuard.class);

    private final DiscordProperties properties;
    private final Environment environment;

    public DiscordStartupGuard(DiscordProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void check() {
        boolean local = !environment.acceptsProfiles(Profiles.of("dev | prod"));

        if (!properties.verifySignature()) {
            if (!local) {
                throw new IllegalStateException("app.discord.verify-signature=false 는 local 에서만 허용된다.");
            }
            log.warn("디스코드 요청 서명 검증이 꺼져 있다(local). 외부에 노출하지 말 것.");
        } else if (isBlank(properties.publicKey())) {
            throw new IllegalStateException("서명 검증이 켜져 있는데 app.discord.public-key(DISCORD_PUBLIC_KEY)가 비어 있다.");
        }

        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        if (!prod && !isBlank(properties.productionApplicationId())
                && properties.productionApplicationId().equals(properties.applicationId())) {
            throw new IllegalStateException(
                    "prod 가 아닌데 운영 봇 애플리케이션 ID(" + properties.applicationId() + ")로 기동했다. 토큰을 확인할 것.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
