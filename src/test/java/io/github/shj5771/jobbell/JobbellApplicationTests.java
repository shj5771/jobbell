package io.github.shj5771.jobbell;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Flyway 마이그레이션이 실제 MySQL 에 적용되고, 그 스키마로 JPA 검증(ddl-auto=validate)까지 통과하는지 본다. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JobbellApplicationTests {

    @Test
    void contextLoads() {
    }
}
