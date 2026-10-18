package io.github.shj5771.jobbell.posting.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.shj5771.jobbell.posting.Company;
import io.github.shj5771.jobbell.posting.RawPosting;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Greenhouse 채용 보드의 공개 API. 기업이 공개용으로 내놓은 엔드포인트라 인증이 없다.
 * {@code company.source_key} 는 보드 이름이다(예: {@code https://job-boards.greenhouse.io/<보드 이름>}).
 *
 * <p>경력 조건과 마감일은 이 API 에 없다. 둘 다 null 로 넘기고, 매칭은 "모름" 으로 취급한다.
 */
public class GreenhouseSource implements PostingSource {

    public static final String SOURCE_ID = "greenhouse";

    private final RestClient restClient;

    public GreenhouseSource(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String sourceId() {
        return SOURCE_ID;
    }

    @Override
    public List<RawPosting> fetch(Company company) {
        Jobs response;
        try {
            response = restClient.get()
                    .uri("/v1/boards/{board}/jobs", company.getSourceKey())
                    .retrieve()
                    .body(Jobs.class);
        } catch (RestClientResponseException e) {
            throw new PostingSourceException(
                    "Greenhouse " + e.getStatusCode().value() + " 응답: board=" + company.getSourceKey(), e);
        } catch (RestClientException e) {
            throw new PostingSourceException("Greenhouse 호출 실패: board=" + company.getSourceKey(), e);
        }

        // jobs 가 아예 없으면 빈 목록이 아니라 형식 변경이다. 여기서 빈 목록을 돌려주면 조용히 묻힌다.
        if (response == null || response.jobs() == null) {
            throw new PostingSourceException("Greenhouse 응답에 jobs 필드가 없다: board=" + company.getSourceKey());
        }

        return response.jobs().stream()
                .map(job -> new RawPosting(String.valueOf(job.id()), job.title().strip(), null, null, job.absoluteUrl()))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Jobs(List<Job> jobs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Job(long id, String title, @JsonProperty("absolute_url") String absoluteUrl) {
    }
}
