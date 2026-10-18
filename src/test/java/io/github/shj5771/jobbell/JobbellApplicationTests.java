package io.github.shj5771.jobbell;

import org.junit.jupiter.api.Test;

/** Flyway 마이그레이션이 실제 MySQL 에 적용되고, 그 스키마로 JPA 검증(ddl-auto=validate)까지 통과하는지 본다. */
@IntegrationTest
class JobbellApplicationTests {

    @Test
    void contextLoads() {
    }
}
