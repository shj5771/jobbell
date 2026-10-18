package io.github.shj5771.jobbell;

import io.github.shj5771.jobbell.support.TestDiscordKeys;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 실제 MySQL(Testcontainers)로 앱 전체를 띄우는 테스트.
 *
 * <p>모든 통합 테스트가 이 애너테이션 하나만 쓰게 해서 설정 조합을 하나로 묶는다.
 * 조합이 다르면 스프링 테스트 캐시가 컨텍스트(와 MySQL 컨테이너)를 따로 만든다.
 * 스케줄러는 끈다. 테스트가 직접 부른 수집과 스케줄러가 돌린 수집이 섞이면 결과를 믿을 수 없다.
 * 디스코드 서명 검증은 켠다. local 프로파일은 꺼져 있지만, 테스트는 운영과 같은 경로를 타야 한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "app.discord.verify-signature=true",
        "app.discord.public-key=" + TestDiscordKeys.PUBLIC_KEY_HEX})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {
}
