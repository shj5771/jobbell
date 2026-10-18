package io.github.shj5771.jobbell.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 현재 시각은 이 빈으로만 얻는다. 테스트에서 시각을 고정하거나 흘려보낼 수 있어야 한다. */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
