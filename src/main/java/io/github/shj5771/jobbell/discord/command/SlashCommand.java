package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;

import java.util.List;
import java.util.Map;

/** 슬래시 명령 하나. {@link #definition()} 은 디스코드에 등록하는 형식 그대로다. */
public interface SlashCommand {

    String name();

    Map<String, Object> definition();

    default boolean adminOnly() {
        return false;
    }

    /** 3초 안에 끝나야 한다. DB 조회·저장만 하고 외부 호출은 하지 않는다. */
    Map<String, Object> handle(long guildId, Interaction interaction);

    default List<Map<String, String>> autocomplete(long guildId, Interaction interaction) {
        return List.of();
    }
}
