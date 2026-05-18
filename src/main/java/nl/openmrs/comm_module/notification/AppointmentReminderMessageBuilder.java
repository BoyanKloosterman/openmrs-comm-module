package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** US-001-3: bouwt queue-bericht met datum, tijd, locatie en instructies. */
@Component
public class AppointmentReminderMessageBuilder {

    public static final String MESSAGE_TYPE_24H = "APPOINTMENT_REMINDER_24H";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("nl-NL"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationSchedulerProperties schedulerProperties;

    public AppointmentReminderMessageBuilder(NotificationSchedulerProperties schedulerProperties) {
        this.schedulerProperties = schedulerProperties;
    }

    public Optional<NotificationQueueMessage> build24HourReminder(PolledEncounterEntity encounter) {
        String phone = encounter.getPatientPhone();
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }

        ZoneId zone = ZoneId.of(schedulerProperties.getReminderZoneId());
        ZonedDateTime appointment = encounter.getEncounterDatetime().atZone(zone);

        String subject = "Herinnering: afspraak over 24 uur";
        String body = buildBody(encounter, appointment);

        NotificationQueueMessage message = new NotificationQueueMessage(
                UUID.randomUUID(),
                phone.trim(),
                subject,
                body,
                schedulerProperties.getDefaultProvider(),
                MESSAGE_TYPE_24H,
                Instant.now());

        return Optional.of(message);
    }

    private String buildBody(PolledEncounterEntity encounter, ZonedDateTime appointment) {
        String name = encounter.getPatientDisplayName();
        String greeting = (name != null && !name.isBlank()) ? "Beste " + name.trim() : "Beste patiënt";

        StringBuilder sb = new StringBuilder();
        sb.append(greeting).append(",\n\n");
        sb.append("U heeft over 24 uur een afspraak:\n");
        sb.append("Datum: ").append(DATE_FORMAT.format(appointment)).append('\n');
        sb.append("Tijd: ").append(TIME_FORMAT.format(appointment)).append('\n');
        sb.append("Locatie: ").append(formatLocation(encounter)).append('\n');
        String instructions = formatInstructions(encounter);
        if (!instructions.isBlank()) {
            sb.append("Instructies: ").append(instructions).append('\n');
        }
        return sb.toString().trim();
    }

    private String formatLocation(PolledEncounterEntity encounter) {
        String locationId = encounter.getLocationId();
        if (locationId != null && !locationId.isBlank()) {
            return locationId.trim();
        }
        return "nog niet bekend";
    }

    private String formatInstructions(PolledEncounterEntity encounter) {
        StringBuilder parts = new StringBuilder();
        String type = encounter.getEncounterType();
        if (type != null && !type.isBlank()) {
            parts.append(type.trim());
        }
        String defaults = schedulerProperties.getDefaultInstructions();
        if (defaults != null && !defaults.isBlank()) {
            if (!parts.isEmpty()) {
                parts.append(". ");
            }
            parts.append(defaults.trim());
        }
        return parts.toString();
    }
}
