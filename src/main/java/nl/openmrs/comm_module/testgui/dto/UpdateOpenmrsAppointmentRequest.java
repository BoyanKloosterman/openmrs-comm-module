package nl.openmrs.comm_module.testgui.dto;

/** Wijzig OpenMRS-afspraak (test-GUI). */
public record UpdateOpenmrsAppointmentRequest(
        String reason,
        String status,
        String appointmentStart,
        String locationUuid,
        Boolean runSyncAfter,
        Boolean runPollAfter) {}
