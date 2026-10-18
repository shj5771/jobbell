package io.github.shj5771.jobbell;

import io.github.shj5771.jobbell.support.MutableClock;
import io.github.shj5771.jobbell.support.RecordingDiscordClient;
import io.github.shj5771.jobbell.support.RecordingOpsSink;
import io.github.shj5771.jobbell.support.ResettableOpsAlerter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

/** 테스트용 MySQL·시계·디스코드 클라이언트·운영 경보. MySQL 은 compose.yaml 과 같은 버전을 쓴다. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(Instant.now());
    }

    @Bean
    @Primary
    RecordingDiscordClient recordingDiscordClient() {
        return new RecordingDiscordClient();
    }

    @Bean
    RecordingOpsSink recordingOpsSink() {
        return new RecordingOpsSink();
    }

    /** 중복 거르기는 실제 구현 그대로 두고, 전송만 기록으로 바꾼다. */
    @Bean
    @Primary
    ResettableOpsAlerter recordingOpsAlerter(RecordingOpsSink sink) {
        return new ResettableOpsAlerter(sink);
    }
}
