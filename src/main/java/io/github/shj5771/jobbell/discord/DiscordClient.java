package io.github.shj5771.jobbell.discord;

import java.util.List;
import java.util.Map;

/** 디스코드 REST API 중 이 앱이 쓰는 것. local 은 보내지 않고 로그로만 남기는 구현을 쓴다. */
public interface DiscordClient {

    /**
     * @return 만들어진 메시지 ID
     * @throws RateLimitedException 429. 기다릴 시간을 담는다
     * @throws DiscordApiException  그 밖의 실패
     */
    long createMessage(long channelId, Map<String, Object> message);

    /** 전역 슬래시 명령을 통째로 덮어쓴다. 코드에서 지운 명령은 디스코드에서도 사라진다. */
    void overwriteCommands(List<Map<String, Object>> definitions);
}
