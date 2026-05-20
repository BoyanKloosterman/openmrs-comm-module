package nl.openmrs.comm_module.message_log.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface MessageLogRepository extends JpaRepository<MessageLogEntity, Long> {

    long deleteBySentAtBefore(Instant cutoff);
}
