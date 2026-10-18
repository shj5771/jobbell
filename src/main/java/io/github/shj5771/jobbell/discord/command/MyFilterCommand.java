package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;
import io.github.shj5771.jobbell.discord.Responses;
import io.github.shj5771.jobbell.guild.MemberFilter;
import io.github.shj5771.jobbell.guild.MemberFilterRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MyFilterCommand implements SlashCommand {

    private static final int MAX_KEYWORD = 50;

    private final MemberFilterRepository filters;

    public MyFilterCommand(MemberFilterRepository filters) {
        this.filters = filters;
    }

    @Override
    public String name() {
        return "내조건";
    }

    @Override
    public Map<String, Object> definition() {
        Map<String, Object> keyword = Definitions.option(Definitions.STRING, "키워드", "공고 제목에 들어갈 말. 비우면 조건을 지웁니다", false);
        keyword.put("max_length", MAX_KEYWORD);
        Map<String, Object> career = Definitions.option(Definitions.STRING, "경력", "경력 조건", false);
        career.put("choices", List.of(choice("신입"), choice("경력")));
        return Definitions.command(name(), "조건에 맞는 공고가 올라오면 멘션을 받습니다", false, List.of(keyword, career));
    }

    @Override
    @Transactional
    public Map<String, Object> handle(long guildId, Interaction interaction) {
        MemberFilter.Id id = new MemberFilter.Id(guildId, interaction.userId());
        Optional<String> keyword = interaction.option("키워드").map(String::strip).filter(value -> !value.isEmpty());
        String career = interaction.option("경력").orElse(null);

        if (keyword.isEmpty()) {
            filters.deleteById(id);
            return Responses.ephemeral("조건을 지웠습니다. 더는 멘션하지 않습니다.");
        }
        filters.findById(id).ifPresentOrElse(
                filter -> filter.change(keyword.get(), career),
                () -> filters.save(new MemberFilter(guildId, interaction.userId(), keyword.get(), career)));
        return Responses.ephemeral("조건 저장: 제목에 '" + keyword.get() + "'"
                + (career == null ? "" : ", 경력 '" + career + "'") + " — 맞는 공고가 올라오면 멘션합니다.");
    }

    private static Map<String, Object> choice(String value) {
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("name", value);
        choice.put("value", value);
        return choice;
    }
}
