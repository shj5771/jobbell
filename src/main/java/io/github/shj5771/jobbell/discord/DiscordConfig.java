package io.github.shj5771.jobbell.discord;

import io.github.shj5771.jobbell.common.TraceIdFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

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

    /** 실제로 보내는 클라이언트는 dev/prod 뿐이다. local 은 어떤 토큰을 넣어도 보내지 않는다. */
    @Bean
    @Profile({"dev", "prod"})
    public DiscordClient restDiscordClient(RestClient.Builder builder, DiscordProperties properties) {
        if (properties.botToken() == null || properties.botToken().isBlank()) {
            throw new IllegalStateException("app.discord.bot-token(DISCORD_BOT_TOKEN)이 비어 있다.");
        }
        Duration timeout = Duration.ofSeconds(10);
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);
        RestClient restClient = builder
                .baseUrl(properties.apiBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bot " + properties.botToken())
                // 디스코드는 이 형식의 User-Agent 를 요구한다.
                .defaultHeader(HttpHeaders.USER_AGENT, "DiscordBot (https://github.com/shj5771/jobbell, 0.1)")
                .build();
        return new RestDiscordClient(restClient, properties.applicationId());
    }

    @Bean
    @Profile("!dev & !prod")
    public DiscordClient loggingDiscordClient() {
        return new LoggingDiscordClient();
    }
}
