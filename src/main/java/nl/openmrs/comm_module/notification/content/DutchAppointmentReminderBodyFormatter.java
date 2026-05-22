package nl.openmrs.comm_module.notification.content;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** NL-bericht: datum, tijd, locatie en optionele instructies. */
@Component
public class DutchAppointmentReminderBodyFormatter implements AppointmentReminderBodyFormatter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("nl-NL"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String formatBody(String greeting, String leadLabel, AppointmentNotificationContent content) {
        StringBuilder sb = new StringBuilder();
        sb.append(greeting).append(",\n\n");
        sb.append("U heeft over ").append(leadLabel).append(" een afspraak:\n");
        sb.append("Datum: ").append(DATE_FORMAT.format(content.appointmentTime())).append('\n');
        sb.append("Tijd: ").append(TIME_FORMAT.format(content.appointmentTime())).append('\n');
        sb.append("Locatie: ").append(content.locationOrDefault()).append('\n');
        if (content.hasInstructions()) {
            sb.append("Instructies: ").append(content.instructions().trim()).append('\n');
        }
        return sb.toString().trim();
    }
}
