package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** US-013: instellingen voor dataretentie en opruimtaak. */
@Component("dataRetentionProperties")
@ConfigurationProperties(prefix = "comm.data-retention")
public class DataRetentionProperties {

    private boolean enabled = true;

    /** Dagen na afspraak om persoonsgegevens te verwijderen. */
    private int personalRetentionDays = 14;

    /** Dagen om meta-informatie te bewaren. */
    private int metadataRetentionDays = 365;

    /** Dagelijkse cron voor de opruimtaak. */
    private String cleanupCron = "0 15 2 * * *";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPersonalRetentionDays() {
        return personalRetentionDays;
    }

    public void setPersonalRetentionDays(int personalRetentionDays) {
        this.personalRetentionDays = personalRetentionDays;
    }

    public int getMetadataRetentionDays() {
        return metadataRetentionDays;
    }

    public void setMetadataRetentionDays(int metadataRetentionDays) {
        this.metadataRetentionDays = metadataRetentionDays;
    }

    public String cleanupCron() {
        return cleanupCron;
    }

    public void setCleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
    }
}
