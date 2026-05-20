package nl.openmrs.comm_module.testgui.dto;

public record CreateTestAppointmentRequest(
        String patientUuid,
        String locationUuid,
        String reason,
        Boolean runSyncAfter,
        Boolean runPollAfter,
        /** Lead-uren voor afspraakstart; null = 24 (US-001-test), 1 = 1u-venster (US-002). */
        Integer leadHours) {}
