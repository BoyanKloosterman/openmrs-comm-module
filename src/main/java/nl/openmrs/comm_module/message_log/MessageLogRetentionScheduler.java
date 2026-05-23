package nl.openmrs.comm_module.message_log;

import nl.openmrs.comm_module.config.MessageLogRetentionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** US-014: geplande opruimtaak voor message log metadata. */
@Component
public class MessageLogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageLogRetentionScheduler.class);

    private final MessageLogRetentionProperties properties;
    private final MessageLogRetentionService retentionService;

    public MessageLogRetentionScheduler(
            MessageLogRetentionProperties properties,
            MessageLogRetentionService retentionService) {
        this.properties = properties;
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "#{@messageLogRetentionProperties.cleanupCron()}")
    public void purgeMessageLogs() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            long removed = retentionService.purgeExpiredLogs();
            if (removed > 0) {
                log.info("MessageLog opruiming: {} regels verwijderd", removed);
            }
        } catch (RuntimeException e) {
            log.error("MessageLog opruiming mislukt: {}", e.getMessage(), e);
        }
    }
}
