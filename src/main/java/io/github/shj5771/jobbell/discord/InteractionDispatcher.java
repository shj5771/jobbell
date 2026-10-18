package io.github.shj5771.jobbell.discord;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Map;

/** 인터랙션 종류별로 처리기를 고른다. 명령은 다음 커밋에서 붙는다. */
@Component
public class InteractionDispatcher {

    public Map<String, Object> dispatch(JsonNode interaction) {
        return Responses.ephemeral("아직 지원하지 않는 명령입니다.");
    }
}
