package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryLogViewDto(
        Long id,
        UUID notificationId,
        String appointmentFhirId,
        String messageType,
        String provider,
        String status,
        boolean successful,
        String providerMessageId,
        String errorMessage,
        Instant attemptedAt) {}
