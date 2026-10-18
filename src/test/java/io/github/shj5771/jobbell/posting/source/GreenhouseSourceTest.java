package io.github.shj5771.jobbell.posting.source;

import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.RawPosting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GreenhouseSourceTest {

    private static final Company COMPANY = new Company("디스코드", GreenhouseSource.SOURCE_ID, "discord");
    private static final String URL = "https://boards.test/v1/boards/discord/jobs";

    private MockRestServiceServer server;
    private GreenhouseSource source;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://boards.test");
        server = MockRestServiceServer.bindTo(builder).build();
        source = new GreenhouseSource(builder.build());
    }

    @Test
    @DisplayName("응답의 공고를 출처 공고로 옮긴다. 모르는 필드는 무시한다")
    void 공고를_옮긴다() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"jobs":[{"id":8806482002,"title":" Backend Engineer ","absolute_url":"https://job-boards.greenhouse.io/discord/jobs/8806482002",
                          "location":{"name":"San Francisco"},"updated_at":"2026-10-15T16:08:17-04:00"}],
                 "meta":{"total":1}}
                """, MediaType.APPLICATION_JSON));

        List<RawPosting> postings = source.fetch(COMPANY);

        assertThat(postings).containsExactly(new RawPosting(
                "8806482002", "Backend Engineer", null, null, "https://job-boards.greenhouse.io/discord/jobs/8806482002"));
    }

    @Test
    @DisplayName("열린 공고가 없으면 빈 목록이다 — 이건 정상이다")
    void 열린_공고가_없으면_빈_목록() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"jobs\":[]}", MediaType.APPLICATION_JSON));

        assertThat(source.fetch(COMPANY)).isEmpty();
    }

    @Test
    @DisplayName("jobs 필드가 없으면 빈 목록이 아니라 실패다")
    void jobs_필드가_없으면_실패() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"postings\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> source.fetch(COMPANY))
                .isInstanceOf(PostingSourceException.class)
                .hasMessageContaining("jobs");
    }

    @Test
    @DisplayName("보드가 없으면(404) 상태 코드를 담아 실패한다")
    void 보드가_없으면_실패() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> source.fetch(COMPANY))
                .isInstanceOf(PostingSourceException.class)
                .hasMessageContaining("404")
                .hasMessageContaining("discord");
    }
}
