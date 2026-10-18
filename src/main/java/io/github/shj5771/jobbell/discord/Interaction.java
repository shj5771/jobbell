package io.github.shj5771.jobbell.discord;

import tools.jackson.databind.JsonNode;

import java.math.BigInteger;
import java.util.Optional;

/** 디스코드 인터랙션 본문을 읽기 쉽게 감싼다. 필요한 필드만 꺼낸다. */
public record Interaction(JsonNode json) {

    public static final int APPLICATION_COMMAND = 2;
    public static final int MESSAGE_COMPONENT = 3;
    public static final int AUTOCOMPLETE = 4;

    private static final BigInteger ADMINISTRATOR = BigInteger.ONE.shiftLeft(3);
    private static final BigInteger MANAGE_GUILD = BigInteger.ONE.shiftLeft(5);

    public int type() {
        return json.path("type").asInt();
    }

    /** DM 에서 온 인터랙션이면 비어 있다. */
    public Optional<Long> guildId() {
        return json.hasNonNull("guild_id") ? Optional.of(json.get("guild_id").asLong()) : Optional.empty();
    }

    public long userId() {
        JsonNode user = json.path("member").path("user");
        return (user.isMissingNode() ? json.path("user") : user).path("id").asLong();
    }

    /** 서버 관리 권한. 명령 등록 시에도 막지만, 서버 관리자가 명령 권한을 바꿀 수 있으므로 서버에서 다시 본다. */
    public boolean canManageGuild() {
        String permissions = json.path("member").path("permissions").asString("0");
        BigInteger bits = new BigInteger(permissions.isEmpty() ? "0" : permissions);
        return bits.and(ADMINISTRATOR).signum() != 0 || bits.and(MANAGE_GUILD).signum() != 0;
    }

    public String commandName() {
        return json.path("data").path("name").asString("");
    }

    public String customId() {
        return json.path("data").path("custom_id").asString("");
    }

    public Optional<String> option(String name) {
        for (JsonNode option : json.path("data").path("options")) {
            if (name.equals(option.path("name").asString())) {
                JsonNode value = option.path("value");
                return value.isMissingNode() || value.isNull() ? Optional.empty() : Optional.of(value.asString());
            }
        }
        return Optional.empty();
    }

    /** 자동완성 중인 옵션에 사용자가 지금까지 친 글자. */
    public String focusedValue() {
        for (JsonNode option : json.path("data").path("options")) {
            if (option.path("focused").asBoolean(false)) {
                return option.path("value").asString("");
            }
        }
        return "";
    }
}
