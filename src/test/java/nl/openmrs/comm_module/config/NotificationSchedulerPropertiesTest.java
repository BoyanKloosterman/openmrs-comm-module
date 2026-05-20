package nl.openmrs.comm_module.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationSchedulerPropertiesTest {

    @Test
    void checkDelayMinimaalEenMinuut() {
        NotificationSchedulerProperties p = new NotificationSchedulerProperties();
        p.setCheckIntervalMinutes(0);
        assertEquals(60_000L, p.checkDelayMillis());
    }

    @Test
    void checkDelayVolgtInterval() {
        NotificationSchedulerProperties p = new NotificationSchedulerProperties();
        p.setCheckIntervalMinutes(5);
        assertEquals(5 * 60_000L, p.checkDelayMillis());
    }

    @Test
    void reminderDefaults() {
        NotificationSchedulerProperties p = new NotificationSchedulerProperties();
        assertEquals(24, p.getReminderLeadHours());
        assertEquals(1, p.getReminder1LeadHours());
        assertEquals(60, p.getReminderWindowMinutes());
    }
}
