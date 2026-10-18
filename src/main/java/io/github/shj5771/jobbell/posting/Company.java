package io.github.shj5771.jobbell.posting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** 공고를 가져올 수 있는 기업. 어느 출처({@link #sourceId})의 어떤 키({@link #sourceKey})로 조회하는지를 함께 가진다. */
@Entity
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sourceId;

    @Column(nullable = false)
    private String sourceKey;

    protected Company() {
    }

    public Company(String name, String sourceId, String sourceKey) {
        this.name = name;
        this.sourceId = sourceId;
        this.sourceKey = sourceKey;
    }

    public void rename(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceKey() {
        return sourceKey;
    }
}
