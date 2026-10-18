package io.github.shj5771.jobbell.common;

/**
 * MDC 키는 여기서만 정의한다.
 *
 * <p>키 이름은 로그 패턴의 {@code %X{...}}, JSON 필드명, 로그 검색 쿼리와 <b>문자열로만</b> 이어진다.
 * 한 곳에 리터럴로 {@code "postingid"} 라고 적으면 컴파일도 기동도 성공하고, 그 필드만 조용히 비어 나간다.
 */
public final class MdcKeys {

    /** 작업 하나(명령 1건, 수집 1회, 발송 1건)를 끝까지 잇는 식별자. 32 hex. */
    public static final String TRACE_ID = "traceId";

    /** 수집 스케줄러 1회 실행. 한 번의 실행이 여러 기업을 돌므로 traceId 와 따로 둔다. */
    public static final String RUN_ID = "runId";

    /** 디스코드 서버. */
    public static final String GUILD_ID = "guildId";

    /** 디스코드 사용자. */
    public static final String USER_ID = "userId";

    /** 슬래시 명령 이름. */
    public static final String COMMAND = "command";

    /** 공고 출처 어댑터({@code PostingSource#sourceId()}). */
    public static final String SOURCE_ID = "sourceId";

    public static final String COMPANY_ID = "companyId";

    /** 알림이 안 왔을 때 수집 → 매칭 → 발송을 잇는 키. 수집 로그와 발송 로그 모두에 싣는다. */
    public static final String POSTING_ID = "postingId";

    private MdcKeys() {
    }
}
