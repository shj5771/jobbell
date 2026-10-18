package io.github.shj5771.jobbell.posting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectRunRepository extends JpaRepository<CollectRun, Long> {

    Optional<CollectRun> findTopByCompanyIdAndStatusOrderByStartedAtDesc(Long companyId, CollectRun.Status status);

    Optional<CollectRun> findTopByCompanyIdOrderByStartedAtDesc(Long companyId);

    List<CollectRun> findTop2ByCompanyIdAndStatusOrderByStartedAtDesc(Long companyId, CollectRun.Status status);

    Optional<CollectRun> findTopByStatusOrderByFinishedAtDesc(CollectRun.Status status);

    boolean existsByCompanyIdAndStatus(Long companyId, CollectRun.Status status);
}
