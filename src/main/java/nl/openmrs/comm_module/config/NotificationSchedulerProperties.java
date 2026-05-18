package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** US-001-1: interval voor notificatie-check; bean-naam voor @Scheduled SpEL. */
@Component("notificationSchedulerProperties")
@ConfigurationProperties(prefix = "comm.notification.scheduler")
public class NotificationSchedulerProperties {

    private boolean enabled = true;

    /** Hoe vaak gecontroleerd wordt welke notificaties verstuurd moeten worden. */
    private int checkIntervalMinutes = 1;

    public long checkDelayMillis() {
        int minutes = Math.max(1, checkIntervalMinutes);
        return minutes * 60L * 1000L;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCheckIntervalMinutes() {
        return checkIntervalMinutes;
    }

    public void setCheckIntervalMinutes(int checkIntervalMinutes) {
        this.checkIntervalMinutes = checkIntervalMinutes;
    }
}
