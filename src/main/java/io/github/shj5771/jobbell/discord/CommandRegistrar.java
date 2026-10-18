package io.github.shj5771.jobbell.discord;

import io.github.shj5771.jobbell.discord.command.SlashCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 기동할 때 코드에 있는 명령 정의로 디스코드 명령을 덮어쓴다. 코드와 디스코드가 어긋나지 않게 하는 유일한 경로다.
 *
 * <p>등록이 실패해도 기동은 계속한다. 이전에 등록된 명령으로 서비스는 돌아가고, 실패는 ERROR 로 남는다.
 */
@Component
@ConditionalOnProperty(name = "app.discord.register-commands", havingValue = "true")
public class CommandRegistrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CommandRegistrar.class);

    private final DiscordClient discord;
    private final List<SlashCommand> commands;

    public CommandRegistrar(DiscordClient discord, List<SlashCommand> commands) {
        this.discord = discord;
        this.commands = commands;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            discord.overwriteCommands(commands.stream().map(SlashCommand::definition).toList());
            log.info("슬래시 명령 {}개 등록", commands.size());
        } catch (DiscordApiException e) {
            log.error("슬래시 명령 등록 실패. 이전 등록본으로 계속한다: {}", e.getMessage(), e);
        }
    }
}
