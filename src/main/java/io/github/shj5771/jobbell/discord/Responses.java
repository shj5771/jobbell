package io.github.shj5771.jobbell.discord;

import java.util.List;
import java.util.Map;

/** 인터랙션 응답 본문. 형식은 디스코드 Interaction Response 를 따른다. */
public final class Responses {

    private static final int CHANNEL_MESSAGE = 4;
    private static final int AUTOCOMPLETE_RESULT = 8;
    private static final int EPHEMERAL = 1 << 6;

    /** 명령을 보낸 사람에게만 보이는 답. 멘션이 있어도 알림을 울리지 않는다. */
    public static Map<String, Object> ephemeral(String content) {
        return Map.of("type", CHANNEL_MESSAGE, "data", Map.of(
                "content", content,
                "flags", EPHEMERAL,
                "allowed_mentions", Map.of("parse", List.of())));
    }

    public static Map<String, Object> choices(List<Map<String, String>> choices) {
        return Map.of("type", AUTOCOMPLETE_RESULT, "data", Map.of("choices", choices));
    }

    private Responses() {
    }
}
