package io.github.shj5771.jobbell.discord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordStartupGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues("app.discord.public-key=abc", "app.discord.verify-signature=true");

    @Test
    @DisplayName("서명 검증을 끄는 것은 local 에서만 된다")
    void 서명_끄기는_local만() {
        runner.withPropertyValues("app.discord.verify-signature=false")
                .run(context -> assertThat(context).hasNotFailed());
        runner.withPropertyValues("app.discord.verify-signature=false", "spring.profiles.active=dev")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("local"));
    }

    @Test
    @DisplayName("서명 검증이 켜져 있는데 공개키가 없으면 실패")
    void 공개키_없음() {
        runner.withPropertyValues("app.discord.public-key=", "spring.profiles.active=dev")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("DISCORD_PUBLIC_KEY"));
    }

    @Test
    @DisplayName("prod 가 아닌데 운영 봇 ID 로 뜨면 실패하고, prod 면 통과한다")
    void 운영_봇_ID() {
        ApplicationContextRunner production = runner.withPropertyValues(
                "app.discord.application-id=111", "app.discord.production-application-id=111");

        production.withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("111"));
        production.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @EnableConfigurationProperties(DiscordProperties.class)
    static class Config {

        @Bean
        DiscordStartupGuard guard(DiscordProperties properties, Environment environment) {
            return new DiscordStartupGuard(properties, environment);
        }
    }
}
