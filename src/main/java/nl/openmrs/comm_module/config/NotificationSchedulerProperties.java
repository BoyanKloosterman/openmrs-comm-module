package nl.openmrs.comm_module.config;

import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** US-001/002: scheduler-interval en herinneringsvensters (24u + 1u). */
@Component("notificationSchedulerProperties")
@ConfigurationProperties(prefix = "comm.notification.scheduler")
public class NotificationSchedulerProperties {

    private boolean enabled = true;

    /** Uren voor de afspraak dat de 24u-herinnering hoort (US-001). */
    private int reminderLeadHours = 24;

    /** Uren voor de afspraak dat de 1u-herinnering hoort (US-002). */
    private int reminder1LeadHours = 1;

    /** Breedte venster rond doeltijd; scheduler hoeft niet exact op de minuut te raken. */
    private int reminderWindowMinutes = 60;

    /** Hoe vaak gecontroleerd wordt welke notificaties verstuurd moeten worden. */
    private int checkIntervalMinutes = 1;

    /** Tijdelijk tot US-008 provider per organisatie kiest. */
    private MessagingProviderType defaultProvider = MessagingProviderType.SWIFTSEND;

    private String defaultInstructions = "Kom 10 minuten van tevoren. Neem uw legitimatie mee.";

    /** Weergave datum/tijd in herinneringstekst (US-011 breidt dit uit). */
    private String reminderZoneId = "Europe/Amsterdam";

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

    public int getReminder1LeadHours() {
        return reminder1LeadHours;
    }

    public void setReminder1LeadHours(int reminder1LeadHours) {
        this.reminder1LeadHours = reminder1LeadHours;
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

    public MessagingProviderType getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(MessagingProviderType defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultInstructions() {
        return defaultInstructions;
    }

    public void setDefaultInstructions(String defaultInstructions) {
        this.defaultInstructions = defaultInstructions;
    }

    public String getReminderZoneId() {
        return reminderZoneId;
    }

    public void setReminderZoneId(String reminderZoneId) {
        this.reminderZoneId = reminderZoneId;
    }
}
