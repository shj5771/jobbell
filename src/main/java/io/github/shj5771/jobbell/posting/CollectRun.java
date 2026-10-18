package io.github.shj5771.jobbell.posting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * 기업 한 곳에 대한 수집 1회. "새 공고 0건" 과 "수집 실패" 를 구분하는 근거다.
 *
 * <p>성공했는데 {@link #fetchedCount} 가 0 인 것과 실패한 것은 둘 다 알림이 없다는 점에서 똑같아 보인다.
 * 이 행이 없으면 둘을 사후에 구분할 방법이 없다.
 */
@Entity
public class CollectRun {

    public enum Status {RUNNING, SUCCESS, FAILED}

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String runId;

    @Column(nullable = false)
    private String sourceId;

    @Column(nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** 응답에 담긴 열린 공고 수. 새 공고 수가 아니다. */
    private Integer fetchedCount;

    private Integer newCount;

    private String error;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    protected CollectRun() {
    }

    public static CollectRun start(String runId, Company company, Instant now) {
        CollectRun run = new CollectRun();
        run.runId = runId;
        run.sourceId = company.getSourceId();
        run.companyId = company.getId();
        run.status = Status.RUNNING;
        run.startedAt = now;
        return run;
    }

    public void succeed(int fetchedCount, int newCount, Instant now) {
        this.status = Status.SUCCESS;
        this.fetchedCount = fetchedCount;
        this.newCount = newCount;
        this.finishedAt = now;
    }

    public void fail(String error, Instant now) {
        this.status = Status.FAILED;
        this.error = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
        this.finishedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getFetchedCount() {
        return fetchedCount;
    }

    public Integer getNewCount() {
        return newCount;
    }

    public String getError() {
        return error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
