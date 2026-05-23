package nl.openmrs.comm_module.testgui.dto;

import java.util.UUID;

public record MessagePreviewDto(
        UUID notificationId,
        String recipient,
        String subject,
        String body,
        String provider,
        String messageType,
        String appointmentFhirId) {}
