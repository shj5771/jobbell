package io.github.shj5771.jobbell.discord;

import io.github.shj5771.jobbell.common.TraceIdFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(DiscordProperties.class)
public class DiscordConfig {

    /** 모든 요청의 맨 앞. 뒤따르는 필터(서명 검증)의 로그에도 traceId 가 붙어야 한다. */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "app.discord.verify-signature", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<SignatureFilter> signatureFilter(DiscordProperties properties, Clock clock) {
        FilterRegistrationBean<SignatureFilter> registration =
                new FilterRegistrationBean<>(new SignatureFilter(new SignatureVerifier(properties.publicKey()), clock));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.addUrlPatterns(InteractionController.PATH);
        return registration;
    }
}
