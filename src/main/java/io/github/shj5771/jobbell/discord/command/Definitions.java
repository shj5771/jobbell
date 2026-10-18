package io.github.shj5771.jobbell.discord.command;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 디스코드 명령 등록 본문을 만드는 도우미. 옵션 타입 번호는 디스코드 API 의 값이다. */
final class Definitions {

    static final int STRING = 3;
    static final int INTEGER = 4;
    static final int CHANNEL = 7;

    private static final int GUILD_TEXT = 0;
    private static final String MANAGE_GUILD = String.valueOf(1 << 5);
    /** 서버 안에서만 쓴다. DM 에서는 명령이 보이지 않는다. */
    private static final List<Integer> GUILD_ONLY = List.of(0);

    static Map<String, Object> command(String name, String description, boolean adminOnly, List<Map<String, Object>> options) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", name);
        definition.put("description", description);
        definition.put("contexts", GUILD_ONLY);
        definition.put("options", options);
        if (adminOnly) {
            definition.put("default_member_permissions", MANAGE_GUILD);
        }
        return definition;
    }

    static Map<String, Object> option(int type, String name, String description, boolean required) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("type", type);
        option.put("name", name);
        option.put("description", description);
        option.put("required", required);
        return option;
    }

    static Map<String, Object> textChannel(String name, String description) {
        Map<String, Object> option = option(CHANNEL, name, description, true);
        option.put("channel_types", List.of(GUILD_TEXT));
        return option;
    }

    static Map<String, Object> autocomplete(String name, String description) {
        Map<String, Object> option = option(STRING, name, description, true);
        option.put("autocomplete", true);
        return option;
    }

    private Definitions() {
    }
}
