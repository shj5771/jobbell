package io.github.shj5771.jobbell.discord;

import java.util.Arrays;
import java.util.stream.Collectors;

/** 테스트용 인터랙션 본문. 디스코드가 보내는 필드 중 이 앱이 읽는 것만 채운다. */
final class Interactions {

    static final long GUILD = 900_000_000_000_000_001L;
    static final long ADMIN = 800_000_000_000_000_001L;
    static final long MEMBER = 800_000_000_000_000_002L;

    private static final String ADMIN_PERMISSIONS = String.valueOf(1 << 5);
    private static final String MEMBER_PERMISSIONS = "0";

    static String command(long userId, String name, String... options) {
        return """
                {"type":2,"guild_id":"%d","member":{"user":{"id":"%d"},"permissions":"%s"},
                 "data":{"name":"%s","options":[%s]}}
                """.formatted(GUILD, userId, permissionsOf(userId), name, joined(options));
    }

    static String autocomplete(long userId, String name, String focusedOption, String typed) {
        return """
                {"type":4,"guild_id":"%d","member":{"user":{"id":"%d"},"permissions":"%s"},
                 "data":{"name":"%s","options":[{"name":"%s","type":3,"value":"%s","focused":true}]}}
                """.formatted(GUILD, userId, permissionsOf(userId), name, focusedOption, typed);
    }

    static String button(long userId, String customId) {
        return """
                {"type":3,"guild_id":"%d","member":{"user":{"id":"%d"},"permissions":"%s"},
                 "data":{"custom_id":"%s","component_type":2}}
                """.formatted(GUILD, userId, permissionsOf(userId), customId);
    }

    static String option(String name, Object value) {
        String json = value instanceof Number ? value.toString() : "\"" + value + "\"";
        return "{\"name\":\"" + name + "\",\"value\":" + json + "}";
    }

    private static String permissionsOf(long userId) {
        return userId == ADMIN ? ADMIN_PERMISSIONS : MEMBER_PERMISSIONS;
    }

    private static String joined(String[] options) {
        return Arrays.stream(options).collect(Collectors.joining(","));
    }

    private Interactions() {
    }
}
