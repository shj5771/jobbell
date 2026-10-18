package io.github.shj5771.jobbell.common;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * blue/green 배포 중에는 앱 컨테이너가 잠깐 둘 뜨고 스케줄러도 둘이 돈다.
 * 같은 수집이 두 번 돌면 발송이 경합한다. 중복 발송은 유니크 제약이 막지만, 수집 자체는 한쪽만 돌게 한다.
 *
 * <p>스케줄링 스위치({@link SchedulingConfig})와 따로 둔다. 테스트가 스케줄러를 꺼도 락은 검증할 수 있어야 한다.
 * 락 시각은 DB 시계를 쓴다. 두 컨테이너의 시계가 어긋나도 판단이 갈리지 않는다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }
}
