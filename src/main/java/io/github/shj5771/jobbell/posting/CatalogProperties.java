package io.github.shj5771.jobbell.posting;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 봇이 지켜보는 기업 목록. 스터디는 이 목록 안에서 관심 기업을 고른다.
 *
 * @param companies 기업 이름, 출처 ID, 출처 안에서의 키
 */
@ConfigurationProperties("app.catalog")
public record CatalogProperties(List<Entry> companies) {

    public CatalogProperties {
        companies = companies == null ? List.of() : List.copyOf(companies);
    }

    public record Entry(String name, String source, String key) {
    }
}
