package io.github.shj5771.jobbell.posting;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    boolean existsBySourceIdAndExternalId(String sourceId, String externalId);
}
