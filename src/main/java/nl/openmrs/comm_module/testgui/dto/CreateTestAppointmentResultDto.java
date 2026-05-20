package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

public record CreateTestAppointmentResultDto(
        String appointmentFhirId,
        int openmrsAppointmentId,
        String patientFhirId,
        Instant appointmentStart,
        String reason,
        String locationName,
        String patientName,
        String syncNote,
        String pollNote,
        PolledAppointmentViewDto polledRow) {}
