package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;
import io.github.shj5771.jobbell.discord.Responses;
import io.github.shj5771.jobbell.guild.Application;
import io.github.shj5771.jobbell.guild.ApplicationRepository;
import io.github.shj5771.jobbell.posting.Posting;
import io.github.shj5771.jobbell.posting.PostingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ApplicationsCommand implements SlashCommand {

    private static final int DEFAULT_DAYS = 30;

    private final ApplicationRepository applications;
    private final PostingRepository postings;
    private final Clock clock;

    public ApplicationsCommand(ApplicationRepository applications, PostingRepository postings, Clock clock) {
        this.applications = applications;
        this.postings = postings;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "지원현황";
    }

    @Override
    public Map<String, Object> definition() {
        Map<String, Object> days = Definitions.option(Definitions.INTEGER, "기간", "최근 며칠(기본 30일)", false);
        days.put("min_value", 1);
        days.put("max_value", 365);
        return Definitions.command(name(), "스터디원별 지원 기록을 봅니다", false, List.of(days));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> handle(long guildId, Interaction interaction) {
        int days = interaction.option("기간").map(Integer::parseInt).orElse(DEFAULT_DAYS);
        List<Application> recent = applications.findByIdGuildIdAndAppliedAtAfterOrderByAppliedAtDesc(
                guildId, clock.instant().minus(Duration.ofDays(days)));
        if (recent.isEmpty()) {
            return Responses.ephemeral("최근 " + days + "일 동안 지원 기록이 없습니다. 알림의 [지원함] 버튼으로 남길 수 있습니다.");
        }

        Map<Long, Posting> postingById = postings.findAllById(recent.stream().map(Application::getPostingId).toList())
                .stream().collect(Collectors.toMap(Posting::getId, Function.identity()));
        Map<Long, List<Application>> byUser = recent.stream()
                .collect(Collectors.groupingBy(Application::getUserId, LinkedHashMap::new, Collectors.toList()));

        StringBuilder content = new StringBuilder("**최근 " + days + "일 지원 현황**\n");
        byUser.forEach((userId, list) -> {
            content.append(Format.mention(userId)).append(' ').append(list.size()).append("건\n");
            for (Application application : list) {
                Posting posting = postingById.get(application.getPostingId());
                content.append("  • ").append(posting.getCompany().getName()).append(" · ").append(posting.getTitle())
                        .append(" (").append(Format.date(application.getAppliedAt())).append(")\n");
            }
        });
        return Responses.ephemeral(Format.clip(content.toString()));
    }
}
