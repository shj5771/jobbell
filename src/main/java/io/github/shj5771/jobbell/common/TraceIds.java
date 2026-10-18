package io.github.shj5771.jobbell.common;

import java.util.UUID;

public final class TraceIds {

    /** 하이픈을 걷어낸 32 hex. W3C trace-context 의 trace-id 폭과 같다. */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 디스코드 메시지 하단에 붙이는 추적 코드. 앞 8자리로 로그를 접두어 검색한다. */
    public static String shortCode(String traceId) {
        return traceId.substring(0, 8);
    }

    private TraceIds() {
    }
}
