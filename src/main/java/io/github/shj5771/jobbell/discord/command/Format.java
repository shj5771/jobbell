package io.github.shj5771.jobbell.discord.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class Format {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("M/d HH:mm").withZone(SEOUL);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d").withZone(SEOUL);

    /** 디스코드 메시지 한 건의 글자 수 한도. */
    static final int MAX_CONTENT = 2000;

    static String dateTime(Instant instant) {
        return DATE_TIME.format(instant);
    }

    static String date(Instant instant) {
        return DATE.format(instant);
    }

    static String mention(long userId) {
        return "<@" + userId + ">";
    }

    static String clip(String content) {
        return content.length() <= MAX_CONTENT ? content : content.substring(0, MAX_CONTENT - 2) + "\n…";
    }

    private Format() {
    }
}
