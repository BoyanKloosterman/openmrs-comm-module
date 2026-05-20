package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

public record PolledAppointmentViewDto(
        String appointmentFhirId,
        Integer openmrsAppointmentId,
        boolean cancellableInOpenmrs,
        String patientFhirId,
        Instant appointmentDatetime,
        String patientDisplayName,
        String patientPhoneMasked,
        String location,
        String appointmentType,
        String appointmentReason,
        boolean voided,
        Instant lastPolledAt,
        AppointmentWindowStatus windowStatus,
        boolean inReminderWindow,
        boolean alreadySentSuccessfully,
        MessagePreviewDto messagePreview) {}
