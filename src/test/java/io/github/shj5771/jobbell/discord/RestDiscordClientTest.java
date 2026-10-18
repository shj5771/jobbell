package io.github.shj5771.jobbell.discord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestDiscordClientTest {

    private MockRestServiceServer server;
    private RestDiscordClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://discord.test/api/v10");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestDiscordClient(builder.build(), "777");
    }

    @Test
    @DisplayName("메시지를 채널에 보내고 만들어진 메시지 ID 를 돌려준다")
    void 메시지_전송() {
        server.expect(requestTo("https://discord.test/api/v10/channels/123/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"content\":\"hi\"}"))
                .andRespond(withSuccess("{\"id\":\"555\"}", MediaType.APPLICATION_JSON));

        assertThat(client.createMessage(123, Map.of("content", "hi"))).isEqualTo(555L);
    }

    @Test
    @DisplayName("429 면 retry_after(초, 소수)를 대기 시간으로 담는다")
    void 호출_한도() {
        server.expect(requestTo("https://discord.test/api/v10/channels/123/messages"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"You are being rate limited.\",\"retry_after\":0.42,\"global\":false}"));

        assertThatThrownBy(() -> client.createMessage(123, Map.of("content", "hi")))
                .isInstanceOfSatisfying(RateLimitedException.class,
                        e -> assertThat(e.retryAfter()).isEqualTo(Duration.ofMillis(420)));
    }

    @Test
    @DisplayName("그 밖의 실패는 상태 코드와 본문을 담는다 — 권한 없는 채널 등")
    void 그_밖의_실패() {
        server.expect(requestTo("https://discord.test/api/v10/channels/123/messages"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Missing Access\",\"code\":50001}"));

        assertThatThrownBy(() -> client.createMessage(123, Map.of("content", "hi")))
                .isInstanceOfSatisfying(DiscordApiException.class, e -> assertThat(e.status()).isEqualTo(403))
                .hasMessageContaining("Missing Access");
    }

    @Test
    @DisplayName("명령 등록은 전역 명령을 통째로 덮어쓴다")
    void 명령_등록() {
        server.expect(requestTo("https://discord.test/api/v10/applications/777/commands"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("[{\"name\":\"기업목록\"}]"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.overwriteCommands(List.of(Map.of("name", "기업목록")));
        server.verify();
    }
}
