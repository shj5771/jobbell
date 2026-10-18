package io.github.shj5771.jobbell.common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로파일별로 root 에 어떤 appender 가 붙는지 고정한다.
 *
 * <p>Logback {@code LoggerContext} 는 JVM 전역이고, Boot 는 이미 초기화돼 있으면 설정 파일을 다시 읽지 않는다.
 * 실측: 같은 JVM 에서 {@code @SpringBootTest} 클래스가 먼저 돌면 그 컨텍스트가 테스트 캐시에 열린 채 남고,
 * 이 클래스의 <b>첫 번째</b> 실행만 local 설정(CONSOLE 하나)을 그대로 물려받아 실패했다.
 * 그 첫 실행이 닫히며 Boot 가 정리를 하므로 두 번째부터는 통과해, 순서에 따라 붙었다 떨어지는 실패가 된다.
 *
 * <p>다른 테스트마다 {@code @DirtiesContext} 를 붙이게 하는 대신, 이 테스트가 매번 초기화 표시를 지우고 시작한다.
 *
 * <p>앱 전체가 아니라 빈 설정 클래스로 띄운다. 로깅 초기화와 application*.yml 적용은 소스와 무관하게 일어나고,
 * 앱 전체를 띄우면 검증과 상관없는 DB 연결이 필요해진다.
 */
class LogbackProfileConfigTest {

    @TempDir
    Path logDir;

    @BeforeEach
    void forgetEarlierLoggingInitialization() {
        LoggingSystem.get(getClass().getClassLoader()).cleanUp();
    }

    @Test
    @DisplayName("local 은 콘솔에만 쓴다")
    void local은_콘솔만() {
        assertThat(rootAppendersWhenStartedWith("local")).containsExactly("CONSOLE");
    }

    @Test
    @DisplayName("프로파일을 주지 않으면 local 과 같다")
    void 프로파일_없음은_local과_같다() {
        assertThat(rootAppendersWhenStartedWith()).containsExactly("CONSOLE");
    }

    @Test
    @DisplayName("dev 는 콘솔과 비동기 파일에 쓴다")
    void dev는_콘솔과_파일() {
        assertThat(rootAppendersWhenStartedWith("dev")).containsExactlyInAnyOrder("CONSOLE", "ASYNC_FILE");
    }

    @Test
    @DisplayName("prod 는 콘솔과 비동기 파일에 쓴다")
    void prod는_콘솔과_파일() {
        assertThat(rootAppendersWhenStartedWith("prod")).containsExactlyInAnyOrder("CONSOLE", "ASYNC_FILE");
    }

    @Test
    @DisplayName("로그 레벨은 yml 이 정한다 — prod 의 root 는 WARN")
    void prod의_root_레벨은_WARN() {
        try (ConfigurableApplicationContext ignored = start("prod")) {
            assertThat(root().getLevel()).hasToString("WARN");
        }
    }

    private List<String> rootAppendersWhenStartedWith(String... profiles) {
        try (ConfigurableApplicationContext ignored = start(profiles)) {
            List<String> names = new ArrayList<>();
            for (Iterator<Appender<ILoggingEvent>> it = root().iteratorForAppenders(); it.hasNext(); ) {
                names.add(it.next().getName());
            }
            return names;
        }
    }

    private ConfigurableApplicationContext start(String... profiles) {
        return new SpringApplicationBuilder(LoggingOnly.class)
                .web(WebApplicationType.NONE)
                .profiles(profiles)
                .properties("app.logging.file=" + logDir.resolve("jobbell.log"))
                .run();
    }

    private Logger root() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        return context.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    @Configuration(proxyBeanMethods = false)
    static class LoggingOnly {
    }
}
