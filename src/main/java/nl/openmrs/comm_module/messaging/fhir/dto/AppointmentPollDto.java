package nl.openmrs.comm_module.messaging.fhir.dto;

import java.time.Instant;

public record AppointmentPollDto(
        String uuid,
        String appointmentId,
        String patientId,
        Instant appointmentDatetime,
        String locationId,
        String appointmentType,
        boolean voided) {}
