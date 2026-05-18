package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** US-001-1/2: scheduler-interval en 24u-herinneringsvenster. */
@Component("notificationSchedulerProperties")
@ConfigurationProperties(prefix = "comm.notification.scheduler")
public class NotificationSchedulerProperties {

    private boolean enabled = true;

    /** Uren voor de afspraak dat de herinnering hoort (US-001: 24). */
    private int reminderLeadHours = 24;

    /** Breedte venster rond doeltijd; scheduler hoeft niet exact op de minuut te raken. */
    private int reminderWindowMinutes = 60;

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

    public int getReminderLeadHours() {
        return reminderLeadHours;
    }

    public void setReminderLeadHours(int reminderLeadHours) {
        this.reminderLeadHours = reminderLeadHours;
    }

    public int getReminderWindowMinutes() {
        return reminderWindowMinutes;
    }

    public void setReminderWindowMinutes(int reminderWindowMinutes) {
        this.reminderWindowMinutes = reminderWindowMinutes;
    }

    public int getCheckIntervalMinutes() {
        return checkIntervalMinutes;
    }

    public void setCheckIntervalMinutes(int checkIntervalMinutes) {
        this.checkIntervalMinutes = checkIntervalMinutes;
    }
}
