package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;
import io.github.shj5771.jobbell.discord.Responses;
import io.github.shj5771.jobbell.guild.Guild;
import io.github.shj5771.jobbell.guild.GuildRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class SetChannelCommand implements SlashCommand {

    private final GuildRepository guilds;

    public SetChannelCommand(GuildRepository guilds) {
        this.guilds = guilds;
    }

    @Override
    public String name() {
        return "채널설정";
    }

    @Override
    public Map<String, Object> definition() {
        return Definitions.command(name(), "새 공고 알림을 올릴 채널을 정합니다", true,
                List.of(Definitions.textChannel("채널", "알림을 올릴 텍스트 채널")));
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    @Transactional
    public Map<String, Object> handle(long guildId, Interaction interaction) {
        long channelId = Long.parseLong(interaction.option("채널").orElseThrow());
        Guild guild = guilds.findById(guildId).orElseThrow();
        guild.setAlertChannel(channelId);
        return Responses.ephemeral("이제 새 공고를 <#" + channelId + "> 에 올립니다.");
    }
}
