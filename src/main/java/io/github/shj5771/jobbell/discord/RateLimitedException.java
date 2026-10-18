package io.github.shj5771.jobbell.discord;

import java.time.Duration;

/** 429. 디스코드가 알려준 시간만큼 기다렸다 다시 보내면 된다. */
public class RateLimitedException extends DiscordApiException {

    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter, Throwable cause) {
        super(429, "디스코드 호출 한도 초과. " + retryAfter.toMillis() + "ms 뒤 재시도", cause);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
