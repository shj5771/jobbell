package io.github.shj5771.jobbell.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** local 전용. 디스코드에 보내지 않고 무엇을 보냈을지만 로그로 남긴다. */
public class LoggingDiscordClient implements DiscordClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingDiscordClient.class);

    private final AtomicLong nextMessageId = new AtomicLong(1);

    @Override
    public long createMessage(long channelId, Map<String, Object> message) {
        long id = nextMessageId.getAndIncrement();
        log.info("[보내지 않음] 채널 {} 메시지 {}: {}", channelId, id, message);
        return id;
    }

    @Override
    public void overwriteCommands(List<Map<String, Object>> definitions) {
        log.info("[보내지 않음] 명령 {}개 등록", definitions.size());
    }
}
