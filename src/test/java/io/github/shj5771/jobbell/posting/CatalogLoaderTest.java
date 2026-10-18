package io.github.shj5771.jobbell.posting;

import io.github.shj5771.jobbell.IntegrationTest;
import io.github.shj5771.jobbell.posting.source.FakePostingSource;
import io.github.shj5771.jobbell.posting.source.PostingSources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class CatalogLoaderTest {

    @Autowired
    CatalogLoader loader;

    @Autowired
    CompanyRepository companies;

    @Test
    @DisplayName("기동 시 카탈로그 기업이 DB 에 들어가고, 다시 적용해도 늘어나지 않는다")
    void 카탈로그가_DB에_들어간다() throws Exception {
        long before = companies.count();
        loader.run(null);

        assertThat(companies.count()).isEqualTo(before);
        assertThat(loader.catalogCompanies()).extracting(Company::getSourceKey)
                .containsExactly("acme", "globex", "broken", "empty");
    }

    @Test
    @DisplayName("이 프로파일에 없는 출처를 적으면 기동이 실패한다")
    void 모르는_출처는_실패() {
        CatalogLoader typo = new CatalogLoader(
                new CatalogProperties(List.of(new CatalogProperties.Entry("오타상사", "greenhose", "x"))),
                new PostingSources(List.of(new FakePostingSource(Clock.systemUTC()))),
                companies);

        assertThatThrownBy(() -> typo.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("greenhose")
                .hasMessageContaining("fake");
    }
}
