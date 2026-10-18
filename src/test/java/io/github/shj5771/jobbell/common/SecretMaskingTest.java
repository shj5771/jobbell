package io.github.shj5771.jobbell.common;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 logback-spring.xml 을 dev 프로파일로 읽어, 콘솔과 JSON 파일 양쪽에서 비밀값이 가려지는지 본다.
 *
 * <p>파일 쓰기는 AsyncAppender 큐를 거친다. 컨텍스트를 닫는다고 큐가 비워졌다는 보장이 없어서,
 * 바로 읽으면 마지막 이벤트가 아직 없을 수 있다(실제로 예외가 붙은 큰 이벤트만 가끔 빠졌다). 기대한 줄이 보일 때까지 다시 읽는다.
 *
 * <p>테스트용 토큰은 실행 중에 조립한다. 진짜 토큰처럼 생긴 문자열을 저장소에 그대로 두면 공개 저장소의
 * 비밀값 스캐너가 걸러낼 수 있고, 그 자체로 나쁜 습관이다.
 */
class SecretMaskingTest {

    private static final String FAKE_BOT_TOKEN = "MTA" + "x".repeat(21) + ".AbCdEf." + "y".repeat(30);
    private static final String FAKE_WEBHOOK = "https://discord.com/api/webhooks/123456789012345678/" + "z".repeat(60);

    @TempDir
    Path logDir;

    @BeforeEach
    void forgetEarlierLoggingInitialization() {
        LoggingSystem.get(getClass().getClassLoader()).cleanUp();
    }

    @Test
    @DisplayName("봇 토큰과 웹훅 URL 은 콘솔과 JSON 파일 모두에서 가려진다. 예외 메시지 안에 있어도 마찬가지다")
    void 비밀값_마스킹() throws IOException {
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(LoggingOnly.class)
                .web(WebApplicationType.NONE)
                .profiles("dev")
                .run("--app.logging.file=" + logDir.resolve("jobbell.log"))) {
            LoggerFactory.getLogger("io.github.shj5771.jobbell.masking")
                    .error("token=" + FAKE_BOT_TOKEN, new IllegalStateException("POST " + FAKE_WEBHOOK + " 실패"));
        } finally {
            System.setOut(original);
        }

        String file = awaitFileContaining("masking");
        String stdout = console.toString(StandardCharsets.UTF_8);
        for (String output : new String[]{file, stdout}) {
            assertThat(output).contains("token=[MASKED]").contains("POST [MASKED]")
                    .doesNotContain(FAKE_BOT_TOKEN).doesNotContain("z".repeat(60));
        }
    }

    @Test
    @DisplayName("traceId(32 hex)나 평범한 문장은 가리지 않는다")
    void 오탐_없음() throws IOException {
        String traceId = TraceIds.newId();
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(LoggingOnly.class)
                .web(WebApplicationType.NONE)
                .profiles("dev")
                .run("--app.logging.file=" + logDir.resolve("jobbell.log"))) {
            LoggerFactory.getLogger("io.github.shj5771.jobbell.masking")
                    .info("trace " + traceId + " io.github.shj5771.jobbell.posting.CollectService 처리 완료");
        }

        assertThat(awaitFileContaining(traceId))
                .contains(traceId).contains("io.github.shj5771.jobbell.posting.CollectService").doesNotContain("[MASKED]");
    }

    private String awaitFileContaining(String text) throws IOException {
        Path file = logDir.resolve("jobbell.log");
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (Files.exists(file) && Files.readString(file).contains(text)) {
                return Files.readString(file);
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return Files.exists(file) ? Files.readString(file) : "";
    }

    @Configuration(proxyBeanMethods = false)
    static class LoggingOnly {
    }
}
