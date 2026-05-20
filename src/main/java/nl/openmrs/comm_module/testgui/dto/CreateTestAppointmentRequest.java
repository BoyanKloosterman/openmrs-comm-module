package nl.openmrs.comm_module.testgui.dto;

public record CreateTestAppointmentRequest(
        String patientUuid,
        String reason,
        Boolean runSyncAfter,
        Boolean runPollAfter) {}
