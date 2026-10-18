package io.github.shj5771.jobbell.discord;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.Map;

/** 디스코드 인터랙션(슬래시 명령·버튼·자동완성)이 들어오는 단일 HTTP 엔드포인트. */
@RestController
public class InteractionController {

    public static final String PATH = "/discord/interactions";

    private static final int PING = 1;

    private final InteractionDispatcher dispatcher;

    public InteractionController(InteractionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping(path = PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> interact(@RequestBody JsonNode interaction) {
        if (interaction.path("type").asInt() == PING) {
            return Map.of("type", 1);
        }
        return dispatcher.dispatch(interaction);
    }
}
