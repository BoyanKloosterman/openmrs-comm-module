package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

public record CreateTestAppointmentResultDto(
        String appointmentFhirId,
        String patientFhirId,
        Instant appointmentStart,
        String phone,
        String patientName,
        String pollNote,
        PolledAppointmentViewDto polledRow) {}
