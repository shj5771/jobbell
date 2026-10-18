package io.github.shj5771.jobbell.posting.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class SourceConfig {

    /** 한 출처가 느려도 다른 기업 수집을 오래 붙잡지 않게 연결·응답 모두 10초로 끊는다. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** 실제 외부 API 는 dev/prod 에서만 부른다. local 은 {@link FakePostingSource} 만 쓴다. */
    @Bean
    @Profile({"dev", "prod"})
    public GreenhouseSource greenhouseSource(RestClient.Builder builder,
                                             @Value("${app.sources.greenhouse.base-url}") String baseUrl) {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT);
        return new GreenhouseSource(builder.baseUrl(baseUrl).requestFactory(requestFactory).build());
    }
}
