package io.github.shj5771.jobbell.notify;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * 서버 하나에 공고 하나를 알리는 발송 1건. (서버, 공고) 유니크 제약이 중복 발송을 막는다.
 *
 * <p>PENDING 을 먼저 쓰고, 보낸 뒤 SENT 로 바꾼다. 보낸 직후 서버가 죽으면 PENDING 이 남고
 * 재시작 후 다시 보내므로 최대 1회 중복될 수 있다. 반대 순서(보낸 뒤 기록)는 유실을 만든다 — 중복이 유실보다 낫다.
 */
@Entity
public class Notification {

    public enum Status {PENDING, SENT, FAILED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long guildId;

    @Column(nullable = false)
    private Long postingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    private Long discordMessageId;

    /** 이 발송을 만든 수집의 traceId. 메시지 하단 추적 코드가 이 값의 앞 8자리다. */
    @Column(nullable = false)
    private String traceId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    protected Notification() {
    }

    public Notification(Long guildId, Long postingId, String traceId, Instant createdAt) {
        this.guildId = guildId;
        this.postingId = postingId;
        this.traceId = traceId;
        this.status = Status.PENDING;
        this.createdAt = createdAt;
    }

    public void attempt() {
        attempts++;
    }

    public void sent(long messageId, Instant now) {
        this.status = Status.SENT;
        this.discordMessageId = messageId;
        this.sentAt = now;
    }

    public void failed() {
        this.status = Status.FAILED;
    }

    public Long getId() {
        return id;
    }

    public Long getGuildId() {
        return guildId;
    }

    public Long getPostingId() {
        return postingId;
    }

    public Status getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Long getDiscordMessageId() {
        return discordMessageId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
