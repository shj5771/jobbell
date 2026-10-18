package io.github.shj5771.jobbell.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Configuration
public class MonitorConfig {

    private static final Logger log = LoggerFactory.getLogger(MonitorConfig.class);

    /**
     * 운영 채널은 디스코드 웹훅이다. 봇 토큰과 분리돼 있어 봇이 고장나도 경보는 나간다.
     * URL 이 비어 있으면(local) 로그로만 남긴다.
     */
    @Bean
    public OpsAlerter opsAlerter(RestClient.Builder builder, @Value("${app.ops.webhook-url:}") String webhookUrl) {
        if (webhookUrl.isBlank()) {
            log.info("운영 웹훅이 없다. 운영 경보는 로그로만 남긴다.");
            return new DedupingOpsAlerter(content -> { });
        }
        RestClient client = builder.build();
        return new DedupingOpsAlerter(content -> client.post()
                .uri(webhookUrl)
                .body(Map.of("content", content, "allowed_mentions", Map.of("parse", List.of())))
                .retrieve()
                .toBodilessEntity());
    }
}
