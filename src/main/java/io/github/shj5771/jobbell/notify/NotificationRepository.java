package io.github.shj5771.jobbell.notify;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByGuildIdAndPostingId(Long guildId, Long postingId);

    List<Notification> findByStatusAndCreatedAtBefore(Notification.Status status, Instant before);
}
