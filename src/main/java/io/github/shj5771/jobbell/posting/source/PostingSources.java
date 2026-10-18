package io.github.shj5771.jobbell.posting.source;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 등록된 출처를 {@link PostingSource#sourceId()} 로 찾는다. 프로파일마다 등록되는 출처가 다르다. */
@Component
public class PostingSources {

    private final Map<String, PostingSource> byId;

    public PostingSources(List<PostingSource> sources) {
        this.byId = sources.stream().collect(Collectors.toUnmodifiableMap(PostingSource::sourceId, Function.identity()));
    }

    public Optional<PostingSource> find(String sourceId) {
        return Optional.ofNullable(byId.get(sourceId));
    }

    public Set<String> ids() {
        return byId.keySet();
    }
}
