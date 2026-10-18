package io.github.shj5771.jobbell.discord;

import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.common.TraceIdFilter;
import io.github.shj5771.jobbell.support.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static io.github.shj5771.jobbell.discord.DiscordRequests.signed;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class InteractionEndpointTest {

    private static final String PING = "{\"type\":1}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MutableClock clock;

    @Test
    @DisplayName("서명이 맞는 PING 에는 PONG 으로 답하고, 응답에 traceId 를 싣는다")
    void 서명된_PING() throws Exception {
        mockMvc.perform(signed(PING, clock.instant()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value(1))
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    @DisplayName("서명 헤더가 없으면 401")
    void 서명_없음() throws Exception {
        mockMvc.perform(post(InteractionController.PATH).contentType(MediaType.APPLICATION_JSON).content(PING))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("본문이 서명 뒤에 바뀌면 401")
    void 본문_변조() throws Exception {
        mockMvc.perform(signed(PING, clock.instant()).content("{\"type\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("서명이 맞아도 타임스탬프가 5분 넘게 지났으면 401 — 재전송 차단")
    void 오래된_요청() throws Exception {
        mockMvc.perform(signed(PING, clock.instant().minus(Duration.ofMinutes(6))))
                .andExpect(status().isUnauthorized());
    }
}
