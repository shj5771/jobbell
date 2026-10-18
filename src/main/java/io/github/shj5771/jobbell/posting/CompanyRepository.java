package io.github.shj5771.jobbell.posting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySourceIdAndSourceKey(String sourceId, String sourceKey);
}
