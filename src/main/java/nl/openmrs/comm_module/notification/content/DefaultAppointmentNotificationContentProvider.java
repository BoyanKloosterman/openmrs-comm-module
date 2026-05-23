package nl.openmrs.comm_module.notification.content;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/** Standaard: polled_appointment + optionele FHIR-fallback voor instructies. */
@Component
public class DefaultAppointmentNotificationContentProvider implements AppointmentNotificationContentProvider {

    private final NotificationSchedulerProperties schedulerProperties;
    private final Optional<OpenmrsFhirOperations> fhirOperations;

    public DefaultAppointmentNotificationContentProvider(
            NotificationSchedulerProperties schedulerProperties,
            Optional<OpenmrsFhirOperations> fhirOperations) {
        this.schedulerProperties = schedulerProperties;
        this.fhirOperations = fhirOperations;
    }

    @Override
    public AppointmentNotificationContent resolve(PolledAppointmentEntity appointment) {
        ZoneId zone = ZoneId.of(schedulerProperties.getReminderZoneId());
        ZonedDateTime time = appointment.getAppointmentDatetime().atZone(zone);
        String location = resolveLocation(appointment);
        String instructions = resolveInstructions(appointment);
        return new AppointmentNotificationContent(time, location, instructions);
    }

    private String resolveLocation(PolledAppointmentEntity appointment) {
        String label = appointment.getLocationId();
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        return null;
    }

    private String resolveInstructions(PolledAppointmentEntity appointment) {
        String reason = appointment.getAppointmentReason();
        if (reason == null || reason.isBlank()) {
            reason = resolveReasonFromFhir(appointment.getAppointmentFhirId());
        }
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        String defaults = schedulerProperties.getDefaultInstructions();
        return defaults == null || defaults.isBlank() ? null : defaults.trim();
    }

    private String resolveReasonFromFhir(String appointmentFhirId) {
        if (fhirOperations.isEmpty() || appointmentFhirId == null || appointmentFhirId.isBlank()) {
            return null;
        }
        try {
            return fhirOperations
                    .get()
                    .readAppointmentByLogicalId(appointmentFhirId)
                    .map(OpenmrsFhirAppointmentMetadata::readReason)
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
