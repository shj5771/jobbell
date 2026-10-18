package io.github.shj5771.jobbell.guild;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Application.Id> {

    List<Application> findByIdGuildIdAndAppliedAtAfterOrderByAppliedAtDesc(Long guildId, Instant after);
}
