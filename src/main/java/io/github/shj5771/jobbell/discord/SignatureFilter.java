package io.github.shj5771.jobbell.discord;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 디스코드 인터랙션 요청의 서명을 검증한다. 실패하면 401 — 디스코드는 엔드포인트 등록 시
 * 잘못된 서명을 일부러 보내 401 이 오는지 확인한다.
 *
 * <p>서명이 맞아도 타임스탬프가 5분 넘게 어긋나면 거부한다. 가로챈 요청을 나중에 다시 보내는 것을 막는다.
 * traceId 필터 뒤에 둬서 거부 로그에도 traceId 가 붙게 한다.
 */
public class SignatureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SignatureFilter.class);
    private static final Duration MAX_SKEW = Duration.ofMinutes(5);

    private final SignatureVerifier verifier;
    private final Clock clock;

    public SignatureFilter(SignatureVerifier verifier, Clock clock) {
        this.verifier = verifier;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CachedBodyRequest cached = new CachedBodyRequest(request, request.getInputStream().readAllBytes());
        String signature = request.getHeader("X-Signature-Ed25519");
        String timestamp = request.getHeader("X-Signature-Timestamp");

        if (signature == null || timestamp == null || !verifier.verify(signature, timestamp, cached.body())) {
            log.warn("서명 검증 실패: 요청을 거부한다");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (isStale(timestamp)) {
            log.warn("서명은 맞지만 타임스탬프가 {} 넘게 어긋난다: {}", MAX_SKEW, timestamp);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(cached, response);
    }

    private boolean isStale(String timestamp) {
        try {
            Instant signedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            return Duration.between(signedAt, clock.instant()).abs().compareTo(MAX_SKEW) > 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
