package io.github.shj5771.jobbell.guild;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.io.Serializable;
import java.time.Instant;

/** [지원함] 버튼 기록. 같은 사람이 같은 공고에 두 번 눌러도 한 건이다(복합 PK). */
@Entity
public class Application {

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private Instant appliedAt;

    protected Application() {
    }

    public Application(Long guildId, Long userId, Long postingId, Instant appliedAt) {
        this.id = new Id(guildId, userId, postingId);
        this.appliedAt = appliedAt;
    }

    public Long getUserId() {
        return id.userId;
    }

    public Long getPostingId() {
        return id.postingId;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    @Embeddable
    public record Id(Long guildId, Long userId, Long postingId) implements Serializable {
    }
}
