package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.hl7.fhir.r5.model.Appointment;
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
    private final OpenmrsFhirOperations fhirOperations;

    public AppointmentReminderMessageBuilder(
            NotificationSchedulerProperties schedulerProperties, OpenmrsFhirOperations fhirOperations) {
        this.schedulerProperties = schedulerProperties;
        this.fhirOperations = fhirOperations;
    }

    public Optional<NotificationQueueMessage> build24HourReminder(PolledAppointmentEntity appointment) {
        String phone = appointment.getPatientPhone();
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }

        ZoneId zone = ZoneId.of(schedulerProperties.getReminderZoneId());
        ZonedDateTime appointmentTime = appointment.getAppointmentDatetime().atZone(zone);

        String subject = "Herinnering: afspraak over 24 uur";
        String body = buildBody(appointment, appointmentTime);

        NotificationQueueMessage message = new NotificationQueueMessage(
                UUID.randomUUID(),
                phone.trim(),
                subject,
                body,
                schedulerProperties.getDefaultProvider(),
                MESSAGE_TYPE_24H,
                Instant.now());
        message.setAppointmentFhirId(appointment.getAppointmentFhirId());

        return Optional.of(message);
    }

    private String buildBody(PolledAppointmentEntity appointment, ZonedDateTime appointmentTime) {
        String name = appointment.getPatientDisplayName();
        String greeting = (name != null && !name.isBlank()) ? "Beste " + name.trim() : "Beste patiënt";

        StringBuilder sb = new StringBuilder();
        sb.append(greeting).append(",\n\n");
        sb.append("U heeft over 24 uur een afspraak:\n");
        sb.append("Datum: ").append(DATE_FORMAT.format(appointmentTime)).append('\n');
        sb.append("Tijd: ").append(TIME_FORMAT.format(appointmentTime)).append('\n');
        sb.append("Locatie: ").append(formatLocation(appointment)).append('\n');
        String instructions = formatInstructions(appointment);
        if (!instructions.isBlank()) {
            sb.append("Instructies: ").append(instructions).append('\n');
        }
        return sb.toString().trim();
    }

    private String formatLocation(PolledAppointmentEntity appointment) {
        String locationId = appointment.getLocationId();
        if (locationId != null && !locationId.isBlank()) {
            return locationId.trim();
        }
        return "nog niet bekend";
    }

    private String formatInstructions(PolledAppointmentEntity appointment) {
        String reason = appointment.getAppointmentReason();
        if (reason == null || reason.isBlank()) {
            reason = resolveReasonFromFhir(appointment.getAppointmentFhirId());
        }
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        StringBuilder parts = new StringBuilder();
        String type = appointment.getAppointmentType();
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

    private String resolveReasonFromFhir(String appointmentFhirId) {
        if (appointmentFhirId == null || appointmentFhirId.isBlank()) {
            return null;
        }
        return fhirOperations
                .readAppointmentByLogicalId(appointmentFhirId)
                .map(OpenmrsFhirAppointmentMetadata::readReason)
                .orElse(null);
    }
}
