package io.github.shj5771.jobbell;

import io.github.shj5771.jobbell.support.MutableClock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

/** 테스트용 MySQL 과 시계. MySQL 은 compose.yaml 과 같은 버전을 쓴다. */
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
}
