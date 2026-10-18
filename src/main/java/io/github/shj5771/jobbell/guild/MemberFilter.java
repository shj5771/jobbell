package io.github.shj5771.jobbell.guild;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.io.Serializable;
import java.util.Locale;

/** 스터디원 한 명의 개인 조건. 맞는 공고가 올라오면 알림에서 멘션된다. */
@Entity
public class MemberFilter {

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private String keyword;

    /** null 이면 경력 조건 없음. */
    private String career;

    protected MemberFilter() {
    }

    public MemberFilter(Long guildId, Long userId, String keyword, String career) {
        this.id = new Id(guildId, userId);
        change(keyword, career);
    }

    public void change(String keyword, String career) {
        this.keyword = keyword.strip();
        this.career = career;
    }

    /**
     * 제목에 키워드가 들어 있고 경력 조건이 맞으면 멘션한다.
     * 공고의 경력을 모르면 멘션한다 — 놓치는 것보다 더 보내는 쪽을 택한다(설계서 5장).
     */
    public boolean matches(String postingTitle, String postingCareer) {
        if (!postingTitle.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return career == null || postingCareer == null || postingCareer.contains(career) || postingCareer.contains("무관");
    }

    public Long getUserId() {
        return id.userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCareer() {
        return career;
    }

    @Embeddable
    public record Id(Long guildId, Long userId) implements Serializable {
    }
}
