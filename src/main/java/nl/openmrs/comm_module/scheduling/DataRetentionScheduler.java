package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.DataRetentionProperties;
import nl.openmrs.comm_module.retention.DataRetentionResult;
import nl.openmrs.comm_module.retention.DataRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** US-013: dagelijkse opruimtaak voor dataretentie. */
@Component
public class DataRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionScheduler.class);

    private final DataRetentionProperties properties;
    private final DataRetentionService dataRetentionService;

    public DataRetentionScheduler(
            DataRetentionProperties properties, DataRetentionService dataRetentionService) {
        this.properties = properties;
        this.dataRetentionService = dataRetentionService;
    }

    @Scheduled(cron = "#{@dataRetentionProperties.cleanupCron()}")
    public void runDataRetentionCleanup() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            DataRetentionResult result = dataRetentionService.runCleanup();
            log.info(
                    "Dataretentie uitgevoerd: {} afspraken geanonimiseerd, {} afspraken verwijderd",
                    result.redactedCount(),
                    result.deletedCount());
        } catch (RuntimeException e) {
            log.error("Dataretentie-opruiming mislukt: {}", e.getMessage(), e);
        }
    }
}
