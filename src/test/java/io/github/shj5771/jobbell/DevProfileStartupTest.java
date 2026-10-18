package io.github.shj5771.jobbell;

import io.github.shj5771.jobbell.discord.RestDiscordClient;
import io.github.shj5771.jobbell.posting.source.GreenhouseSource;
import io.github.shj5771.jobbell.posting.source.PostingSources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 다른 통합 테스트는 local 프로파일로 돈다. 실제 외부 호출 빈(Greenhouse, 디스코드 REST)은 dev/prod 에서만
 * 만들어지므로, 그 빈들이 기동에 실패해도 local 테스트는 전부 통과한다.
 *
 * <p>실제로 그랬다: Boot 4 에서 {@code RestClient.Builder} 자동 설정이 별도 모듈로 빠졌는데 의존성이 없어
 * dev/prod 기동이 실패하는 상태였고, local 테스트 64건은 그걸 잡지 못했다. 그래서 dev 로 한 번 띄운다.
 * 외부로 나가는 호출은 없다 — 명령 등록을 끄고, 수집 스케줄러도 끈다.
 */
@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "app.discord.register-commands=false",
        "app.discord.public-key=" + io.github.shj5771.jobbell.support.TestDiscordKeys.PUBLIC_KEY_HEX,
        "app.discord.bot-token=test-token",
        "app.discord.application-id=1",
        "app.logging.file=build/dev-startup-test/jobbell.log"})
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
class DevProfileStartupTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    PostingSources sources;

    @Test
    @DisplayName("dev 프로파일로 기동하면 실제 출처와 실제 디스코드 클라이언트가 만들어진다")
    void dev_기동() {
        assertThat(sources.ids()).containsExactly(GreenhouseSource.SOURCE_ID);
        assertThat(context.getBean("restDiscordClient")).isInstanceOf(RestDiscordClient.class);
    }
}
