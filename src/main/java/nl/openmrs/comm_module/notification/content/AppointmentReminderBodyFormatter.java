package nl.openmrs.comm_module.notification.content;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Tekstweergave van {@link AppointmentNotificationContent} (vervangbaar voor andere talen/layouts). */
public interface AppointmentReminderBodyFormatter {

    String formatBody(String greeting, String leadLabel, AppointmentNotificationContent content);
}
