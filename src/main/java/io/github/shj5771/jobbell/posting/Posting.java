package io.github.shj5771.jobbell.posting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 한 번이라도 수집된 공고. (출처, 출처의 공고 ID) 쌍이 처음 보인 순간 만들어진다.
 *
 * <p>본문은 저장하지 않는다. 제목·마감·링크만 두고 본문은 공식 페이지로 보낸다.
 */
@Entity
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceId;

    @Column(nullable = false)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String title;

    /** 출처가 경력 조건을 주지 않으면 null. 매칭에서는 "모름" 으로 취급한다. */
    private String career;

    private LocalDate deadline;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Instant firstSeenAt;

    protected Posting() {
    }

    public Posting(Company company, RawPosting raw, Instant firstSeenAt) {
        this.sourceId = company.getSourceId();
        this.externalId = raw.externalId();
        this.company = company;
        this.title = raw.title();
        this.career = raw.career();
        this.deadline = raw.deadline();
        this.url = raw.url();
        this.firstSeenAt = firstSeenAt;
    }

    public boolean isClosedOn(LocalDate today) {
        return deadline != null && deadline.isBefore(today);
    }

    public Long getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getExternalId() {
        return externalId;
    }

    public Company getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getCareer() {
        return career;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getUrl() {
        return url;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }
}
