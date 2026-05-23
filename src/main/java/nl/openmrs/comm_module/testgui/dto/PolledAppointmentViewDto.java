package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

/** Lijstrijen voor test-scheduling.html (alleen velden die de tabel toont). */
public record PolledAppointmentViewDto(
        String appointmentFhirId,
        Instant appointmentDatetime,
        String patientDisplayName,
        String patientPhoneMasked,
        boolean voided,
        AppointmentWindowStatus windowStatus,
        boolean alreadySentSuccessfully,
        AppointmentWindowStatus oneHourWindowStatus,
        boolean alreadySentOneHourSuccessfully) {}
