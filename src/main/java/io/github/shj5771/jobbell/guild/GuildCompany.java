package io.github.shj5771.jobbell.guild;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.io.Serializable;
import java.time.Instant;

/**
 * 스터디가 고른 관심 기업.
 *
 * <p>{@link #addedAt} 이전에 처음 보인 공고는 이 스터디에 알리지 않는다.
 * 기업을 추가한 순간 이미 열려 있던 공고 수십 건이 한꺼번에 쏟아지는 것을 막는다.
 */
@Entity
public class GuildCompany {

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private Instant addedAt;

    protected GuildCompany() {
    }

    public GuildCompany(Long guildId, Long companyId, Instant addedAt) {
        this.id = new Id(guildId, companyId);
        this.addedAt = addedAt;
    }

    public Long getGuildId() {
        return id.guildId;
    }

    public Long getCompanyId() {
        return id.companyId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    @Embeddable
    public record Id(Long guildId, Long companyId) implements Serializable {
    }
}
