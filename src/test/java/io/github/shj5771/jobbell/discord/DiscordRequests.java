package io.github.shj5771.jobbell.discord;

import io.github.shj5771.jobbell.support.TestDiscordKeys;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 디스코드가 보내는 것과 같은 모양의 서명된 인터랙션 요청. */
final class DiscordRequests {

    static MockHttpServletRequestBuilder signed(String body, Instant signedAt) {
        String timestamp = String.valueOf(signedAt.getEpochSecond());
        return post(InteractionController.PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature-Ed25519", TestDiscordKeys.sign(timestamp, body))
                .header("X-Signature-Timestamp", timestamp)
                .content(body);
    }

    private DiscordRequests() {
    }
}
