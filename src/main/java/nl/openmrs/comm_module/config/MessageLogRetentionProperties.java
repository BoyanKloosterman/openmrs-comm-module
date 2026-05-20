package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** US-014: bewaartermijn voor message log metadata (max 1 jaar). */
@Component("messageLogRetentionProperties")
@ConfigurationProperties(prefix = "comm.message-log.retention")
public class MessageLogRetentionProperties {

    private boolean enabled = true;

    private int retentionDays = 365;

    private String cleanupCron = "0 10 3 * * *";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getCleanupCron() {
        return cleanupCron;
    }

    public void setCleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
    }

    public String cleanupCron() {
        return cleanupCron;
    }
}
