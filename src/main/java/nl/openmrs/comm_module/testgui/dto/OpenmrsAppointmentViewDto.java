package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

/** Afspraak uit OpenMRS appointmentscheduling (test-GUI). */
public record OpenmrsAppointmentViewDto(
        int openmrsAppointmentId,
        String appointmentFhirId,
        String status,
        boolean voided,
        Instant appointmentStart,
        Instant appointmentEnd,
        String patientDisplayName,
        String patientUuid,
        String locationName,
        String locationUuid,
        String appointmentType,
        String reason) {}
