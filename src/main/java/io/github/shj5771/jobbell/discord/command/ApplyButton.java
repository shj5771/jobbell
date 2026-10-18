package io.github.shj5771.jobbell.discord.command;

import io.github.shj5771.jobbell.discord.Interaction;
import io.github.shj5771.jobbell.discord.Responses;
import io.github.shj5771.jobbell.guild.Application;
import io.github.shj5771.jobbell.guild.ApplicationRepository;
import io.github.shj5771.jobbell.posting.PostingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/** 알림 메시지의 [지원함] 버튼. custom_id 는 {@code apply:<postingId>}. */
@Component
public class ApplyButton {

    public static final String PREFIX = "apply:";

    private final ApplicationRepository applications;
    private final PostingRepository postings;
    private final Clock clock;

    public ApplyButton(ApplicationRepository applications, PostingRepository postings, Clock clock) {
        this.applications = applications;
        this.postings = postings;
        this.clock = clock;
    }

    public static String customId(long postingId) {
        return PREFIX + postingId;
    }

    public static Optional<Long> postingIdOf(String customId) {
        if (!customId.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String id = customId.substring(PREFIX.length());
        return id.matches("\\d+") ? Optional.of(Long.parseLong(id)) : Optional.empty();
    }

    @Transactional
    public Map<String, Object> press(long guildId, long userId, long postingId) {
        if (!postings.existsById(postingId)) {
            return Responses.ephemeral("공고를 찾을 수 없습니다.");
        }
        Application.Id id = new Application.Id(guildId, userId, postingId);
        if (applications.existsById(id)) {
            return Responses.ephemeral("이미 지원 기록이 있습니다.");
        }
        applications.save(new Application(guildId, userId, postingId, clock.instant()));
        return Responses.ephemeral("지원 기록을 남겼습니다. /지원현황 에서 볼 수 있습니다.");
    }
}
