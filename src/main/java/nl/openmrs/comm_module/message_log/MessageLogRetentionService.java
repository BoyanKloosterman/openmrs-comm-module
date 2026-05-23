package nl.openmrs.comm_module.message_log;

import nl.openmrs.comm_module.config.MessageLogRetentionProperties;
import nl.openmrs.comm_module.message_log.persistence.MessageLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** US-014: verwijder metadata na bewaartermijn. */
@Service
public class MessageLogRetentionService {

    private final MessageLogRepository repository;
    private final MessageLogRetentionProperties properties;
    private final Clock clock;

    public MessageLogRetentionService(
            MessageLogRepository repository,
            MessageLogRetentionProperties properties,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public long purgeExpiredLogs() {
        int retentionDays = properties.getRetentionDays();
        if (retentionDays <= 0) {
            return 0L;
        }
        Instant cutoff = clock.instant().minus(retentionDays, ChronoUnit.DAYS);
        return repository.deleteBySentAtBefore(cutoff);
    }
}
