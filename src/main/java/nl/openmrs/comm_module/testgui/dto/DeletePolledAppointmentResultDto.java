package nl.openmrs.comm_module.testgui.dto;

public record DeletePolledAppointmentResultDto(
        boolean success,
        String appointmentFhirId,
        int deliveryLogsRemoved,
        String message) {}
