package io.github.shj5771.jobbell.support;

import io.github.shj5771.jobbell.discord.DiscordApiException;
import io.github.shj5771.jobbell.discord.DiscordClient;
import io.github.shj5771.jobbell.discord.RateLimitedException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/** 보낸 메시지를 기록하고, 다음 응답을 실패나 429 로 미리 정해둘 수 있는 테스트용 클라이언트. */
public class RecordingDiscordClient implements DiscordClient {

    public record Sent(long channelId, Map<String, Object> message, String thread) {
    }

    private enum Outcome {RATE_LIMITED, FAILED}

    private final List<Sent> sent = new ArrayList<>();
    private final Deque<Outcome> upcoming = new ConcurrentLinkedDeque<>();
    private final AtomicLong nextId = new AtomicLong(1000);

    public void rateLimitNext() {
        upcoming.add(Outcome.RATE_LIMITED);
    }

    public void failNext(int times) {
        for (int i = 0; i < times; i++) {
            upcoming.add(Outcome.FAILED);
        }
    }

    public synchronized List<Sent> sent() {
        return List.copyOf(sent);
    }

    public synchronized void reset() {
        sent.clear();
        upcoming.clear();
    }

    @Override
    public long createMessage(long channelId, Map<String, Object> message) {
        Outcome outcome = upcoming.poll();
        if (outcome == Outcome.RATE_LIMITED) {
            throw new RateLimitedException(Duration.ofMillis(10), null);
        }
        if (outcome == Outcome.FAILED) {
            throw new DiscordApiException(500, "테스트가 정한 실패", null);
        }
        synchronized (this) {
            sent.add(new Sent(channelId, message, Thread.currentThread().getName()));
        }
        return nextId.getAndIncrement();
    }

    @Override
    public void overwriteCommands(List<Map<String, Object>> definitions) {
    }
}
