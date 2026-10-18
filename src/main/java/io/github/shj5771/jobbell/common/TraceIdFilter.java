package io.github.shj5771.jobbell.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * HTTP 요청 하나에 traceId 를 하나 붙이고, 요청이 끝나면 MDC 를 원상복구한다.
 *
 * <p>{@link OncePerRequestFilter} 를 상속하고 ERROR 디스패치도 타게 한다.
 * 미처리 예외는 컨테이너가 {@code /error} 로 다시 디스패치하는데, REQUEST 에만 걸린 필터는 그 구간을 타지 않아
 * 500 응답을 만드는 로그가 전부 NO_TRACE 로 찍힌다. 원래 요청의 traceId 는 request attribute 로 이어받는다.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final String CARRIED_TRACE_ID = TraceIdFilter.class.getName() + ".traceId";

    /** 인바운드 헤더는 외부 입력이다. 개행을 섞어 가짜 로그 줄을 만들 수 없게 영숫자 8~64자만 받는다. */
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[0-9a-zA-Z]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        request.setAttribute(CARRIED_TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try (MdcScope ignored = MdcScope.open()) {
            MDC.put(MdcKeys.TRACE_ID, traceId);
            chain.doFilter(request, response);
        }
    }

    /** 우선순위: ERROR 디스패치로 되돌아온 원래 값 &gt; 인바운드 헤더 &gt; 신규 발급. */
    private String resolveTraceId(HttpServletRequest request) {
        if (request.getAttribute(CARRIED_TRACE_ID) instanceof String carried) {
            return carried;
        }
        String incoming = request.getHeader(TRACE_ID_HEADER);
        if (incoming != null && SAFE_TRACE_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return TraceIds.newId();
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
