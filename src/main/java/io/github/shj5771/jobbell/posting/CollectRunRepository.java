package io.github.shj5771.jobbell.posting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectRunRepository extends JpaRepository<CollectRun, Long> {

    Optional<CollectRun> findTopByCompanyIdAndStatusOrderByStartedAtDesc(Long companyId, CollectRun.Status status);

    Optional<CollectRun> findTopByCompanyIdOrderByStartedAtDesc(Long companyId);

    boolean existsByCompanyIdAndStatus(Long companyId, CollectRun.Status status);
}
