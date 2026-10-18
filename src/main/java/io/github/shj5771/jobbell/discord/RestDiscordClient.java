package io.github.shj5771.jobbell.discord;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RestDiscordClient implements DiscordClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(1);

    private final RestClient restClient;
    private final String applicationId;

    /** {@code restClient} 에는 base URL 과 {@code Authorization: Bot <토큰>} 이 이미 걸려 있어야 한다. */
    public RestDiscordClient(RestClient restClient, String applicationId) {
        this.restClient = restClient;
        this.applicationId = applicationId;
    }

    @Override
    public long createMessage(long channelId, Map<String, Object> message) {
        JsonNode created = call(() -> restClient.post()
                .uri("/channels/{channelId}/messages", channelId)
                .body(message)
                .retrieve()
                .body(JsonNode.class));
        return created.path("id").asLong();
    }

    @Override
    public void overwriteCommands(List<Map<String, Object>> definitions) {
        call(() -> restClient.put()
                .uri("/applications/{applicationId}/commands", applicationId)
                .body(definitions)
                .retrieve()
                .toBodilessEntity());
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new RateLimitedException(retryAfter(e.getResponseBodyAsString()), e);
            }
            throw new DiscordApiException(e.getStatusCode().value(),
                    "디스코드 " + e.getStatusCode().value() + " 응답: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new DiscordApiException(0, "디스코드 호출 실패: " + e.getMessage(), e);
        }
    }

    /** 429 본문의 {@code retry_after} 는 초 단위 소수다. */
    private static Duration retryAfter(String body) {
        try {
            double seconds = JSON.readTree(body).path("retry_after").asDouble(0);
            return seconds > 0 ? Duration.ofMillis((long) Math.ceil(seconds * 1000)) : DEFAULT_RETRY_AFTER;
        } catch (RuntimeException e) {
            return DEFAULT_RETRY_AFTER;
        }
    }
}
