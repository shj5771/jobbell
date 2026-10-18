package io.github.shj5771.jobbell.posting;

import io.github.shj5771.jobbell.posting.source.PostingSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 기동 시 카탈로그를 company 테이블에 맞춘다. 이미 있으면 이름만 갱신하고, 없으면 추가한다.
 *
 * <p>카탈로그에서 빠진 기업은 지우지 않는다. 그 기업의 공고·발송 기록이 남아 있을 수 있다.
 * 대신 수집 대상은 카탈로그 기준이라 더는 수집되지 않는다.
 */
@Component
@EnableConfigurationProperties(CatalogProperties.class)
public class CatalogLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogLoader.class);

    private final CatalogProperties catalog;
    private final PostingSources sources;
    private final CompanyRepository companies;

    public CatalogLoader(CatalogProperties catalog, PostingSources sources, CompanyRepository companies) {
        this.catalog = catalog;
        this.sources = sources;
        this.companies = companies;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> unknownSources = catalog.companies().stream()
                .map(CatalogProperties.Entry::source)
                .filter(source -> sources.find(source).isEmpty())
                .distinct()
                .toList();
        // 출처 이름을 틀리면 그 기업은 영영 수집되지 않는데, 그걸 알려줄 곳이 없다. 기동을 멈춘다.
        if (!unknownSources.isEmpty()) {
            throw new IllegalStateException(
                    "카탈로그에 이 프로파일에서 쓸 수 없는 출처가 있다: " + unknownSources + ". 사용 가능: " + sources.ids());
        }

        for (CatalogProperties.Entry entry : catalog.companies()) {
            companies.findBySourceIdAndSourceKey(entry.source(), entry.key())
                    .ifPresentOrElse(
                            existing -> existing.rename(entry.name()),
                            () -> companies.save(new Company(entry.name(), entry.source(), entry.key())));
        }
        log.info("카탈로그 적용: 기업 {}곳", catalog.companies().size());
    }

    public List<Company> catalogCompanies() {
        return catalog.companies().stream()
                .map(entry -> companies.findBySourceIdAndSourceKey(entry.source(), entry.key()).orElseThrow())
                .toList();
    }
}
