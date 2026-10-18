package io.github.shj5771.jobbell.discord;

import io.github.shj5771.jobbell.common.MdcKeys;
import io.github.shj5771.jobbell.common.TraceIds;
import io.github.shj5771.jobbell.discord.command.ApplyButton;
import io.github.shj5771.jobbell.discord.command.SlashCommand;
import io.github.shj5771.jobbell.guild.Guild;
import io.github.shj5771.jobbell.guild.GuildRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 인터랙션 종류별로 처리기를 고른다.
 *
 * <p>여기서 MDC 에 guildId·userId·command 를 싣는다. 넣기만 하고 빼지 않는 것은 의도다 —
 * 요청 전체를 {@code TraceIdFilter} 의 스코프가 감싸고 있어 요청이 끝나면 통째로 되돌아간다.
 *
 * <p>디스코드는 3초 안에 응답이 없으면 사용자에게 "상호작용 실패" 만 보여주고 끝낸다.
 * 서버 쪽에는 아무 에러도 남지 않으므로, 처리 시간을 재서 한도에 가까우면 WARN 으로 남긴다.
 */
@Component
public class InteractionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(InteractionDispatcher.class);
    private static final long SLOW_MILLIS = 2_000;

    private final Map<String, SlashCommand> commands;
    private final ApplyButton applyButton;
    private final GuildRepository guilds;
    private final Clock clock;

    public InteractionDispatcher(List<SlashCommand> commands, ApplyButton applyButton, GuildRepository guilds, Clock clock) {
        this.commands = commands.stream().collect(Collectors.toUnmodifiableMap(SlashCommand::name, Function.identity()));
        this.applyButton = applyButton;
        this.guilds = guilds;
        this.clock = clock;
    }

    public Map<String, Object> dispatch(JsonNode json) {
        Interaction interaction = new Interaction(json);
        Optional<Long> guildId = interaction.guildId();
        MDC.put(MdcKeys.USER_ID, String.valueOf(interaction.userId()));
        if (guildId.isEmpty()) {
            return Responses.ephemeral("서버 안에서만 쓸 수 있습니다.");
        }
        MDC.put(MdcKeys.GUILD_ID, String.valueOf(guildId.get()));
        ensureGuild(guildId.get());

        long started = System.nanoTime();
        try {
            return route(guildId.get(), interaction);
        } catch (RuntimeException e) {
            // 예외를 그대로 던지면 디스코드는 "상호작용 실패" 만 띄운다. 추적 코드를 주면 문의가 로그로 이어진다.
            log.error("인터랙션 처리 실패", e);
            return Responses.ephemeral("처리 중 오류가 났습니다. 추적 코드: " + TraceIds.shortCode(MDC.get(MdcKeys.TRACE_ID)));
        } finally {
            long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
            if (elapsedMillis > SLOW_MILLIS) {
                log.warn("인터랙션 처리 {}ms — 디스코드 3초 한도에 가깝다", elapsedMillis);
            } else {
                log.info("인터랙션 처리 {}ms", elapsedMillis);
            }
        }
    }

    private Map<String, Object> route(long guildId, Interaction interaction) {
        return switch (interaction.type()) {
            case Interaction.APPLICATION_COMMAND -> command(guildId, interaction);
            case Interaction.AUTOCOMPLETE -> autocomplete(guildId, interaction);
            case Interaction.MESSAGE_COMPONENT -> component(guildId, interaction);
            default -> {
                log.warn("모르는 인터랙션 타입: {}", interaction.type());
                yield Responses.ephemeral("지원하지 않는 요청입니다.");
            }
        };
    }

    private Map<String, Object> command(long guildId, Interaction interaction) {
        MDC.put(MdcKeys.COMMAND, interaction.commandName());
        SlashCommand command = commands.get(interaction.commandName());
        if (command == null) {
            log.warn("등록되지 않은 명령: {}", interaction.commandName());
            return Responses.ephemeral("지원하지 않는 명령입니다.");
        }
        if (command.adminOnly() && !interaction.canManageGuild()) {
            log.info("권한 없는 사용자의 관리 명령 거부");
            return Responses.ephemeral("서버 관리 권한이 있어야 쓸 수 있습니다.");
        }
        log.info("명령 처리: /{}", command.name());
        return command.handle(guildId, interaction);
    }

    private Map<String, Object> autocomplete(long guildId, Interaction interaction) {
        MDC.put(MdcKeys.COMMAND, interaction.commandName());
        SlashCommand command = commands.get(interaction.commandName());
        return Responses.choices(command == null ? List.of() : command.autocomplete(guildId, interaction));
    }

    private Map<String, Object> component(long guildId, Interaction interaction) {
        MDC.put(MdcKeys.COMMAND, "button:" + interaction.customId());
        Optional<Long> postingId = ApplyButton.postingIdOf(interaction.customId());
        if (postingId.isEmpty()) {
            log.warn("모르는 버튼: {}", interaction.customId());
            return Responses.ephemeral("지원하지 않는 버튼입니다.");
        }
        MDC.put(MdcKeys.POSTING_ID, String.valueOf(postingId.get()));
        log.info("지원 버튼");
        return applyButton.press(guildId, interaction.userId(), postingId.get());
    }

    /** 서버 행은 첫 인터랙션에 만든다. 같은 서버의 첫 요청 두 개가 동시에 오면 한쪽 insert 가 PK 에 걸린다 — 무시해도 된다. */
    private void ensureGuild(long guildId) {
        if (guilds.existsById(guildId)) {
            return;
        }
        try {
            guilds.save(new Guild(guildId, clock.instant()));
        } catch (DataIntegrityViolationException alreadyCreated) {
            log.debug("서버 행이 동시에 만들어졌다");
        }
    }
}
