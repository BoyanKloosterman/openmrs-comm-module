package nl.openmrs.comm_module.messaging.fhir.dto;

import java.time.Instant;

public record AppointmentPollDto(
        String uuid,
        String appointmentId,
        String patientId,
        Instant appointmentDatetime,
        /** Leesbare locatie (polikliniek/kamer), geen FHIR Location-id. */
        String locationLabel,
        String appointmentType,
        boolean voided) {}
