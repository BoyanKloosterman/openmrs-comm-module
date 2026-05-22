package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

/** Detail van polled afspraak voor test-GUI (notificatievelden + previews). */
public record PolledAppointmentDetailDto(
        String appointmentFhirId,
        Integer openmrsAppointmentId,
        String patientDisplayName,
        String patientPhoneMasked,
        Instant appointmentDatetime,
        String appointmentDateLabel,
        String appointmentTimeLabel,
        String location,
        String appointmentType,
        String instructions,
        boolean voided,
        MessagePreviewDto messagePreview24h,
        MessagePreviewDto messagePreview1h) {}
