package io.github.shj5771.jobbell.notify;

import io.github.shj5771.jobbell.common.TraceIds;
import io.github.shj5771.jobbell.discord.command.ApplyButton;
import io.github.shj5771.jobbell.posting.Posting;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 새 공고 알림 메시지 본문(디스코드 Create Message 형식). */
final class NotificationMessage {

    private static final int ACTION_ROW = 1;
    private static final int BUTTON = 2;
    private static final int PRIMARY = 1;
    /** 디스코드 allowed_mentions.users 의 상한. */
    private static final int MAX_MENTIONS = 100;

    static Map<String, Object> of(Posting posting, List<Long> mentionedUserIds, String traceId) {
        List<Long> mentions = mentionedUserIds.stream().limit(MAX_MENTIONS).toList();

        StringBuilder content = new StringBuilder("**[새 공고] ")
                .append(posting.getCompany().getName()).append(" · ").append(posting.getTitle()).append("**");
        if (!mentions.isEmpty()) {
            content.append("\n조건 일치: ")
                    .append(mentions.stream().map(id -> "<@" + id + ">").collect(Collectors.joining(" ")));
        }

        List<Map<String, Object>> fields = new ArrayList<>();
        if (posting.getDeadline() != null) {
            fields.add(field("마감", posting.getDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }
        if (posting.getCareer() != null) {
            fields.add(field("경력", posting.getCareer()));
        }

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", posting.getTitle());
        embed.put("url", posting.getUrl());
        embed.put("fields", fields);
        // "이 알림 이상해요" 문의가 오면 이 코드로 로그를 바로 찾는다.
        embed.put("footer", Map.of("text", "추적 코드 " + TraceIds.shortCode(traceId)));

        Map<String, Object> button = new LinkedHashMap<>();
        button.put("type", BUTTON);
        button.put("style", PRIMARY);
        button.put("label", "지원함");
        button.put("custom_id", ApplyButton.customId(posting.getId()));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("content", content.toString());
        message.put("embeds", List.of(embed));
        message.put("components", List.of(Map.of("type", ACTION_ROW, "components", List.of(button))));
        // 본문에 다른 멘션 문자열이 섞여도(@everyone 등) 조건이 맞는 사람만 울리게 명시한다.
        message.put("allowed_mentions", Map.of("users", mentions.stream().map(String::valueOf).toList()));
        return message;
    }

    private static Map<String, Object> field(String name, String value) {
        return Map.of("name", name, "value", value, "inline", true);
    }

    private NotificationMessage() {
    }
}
