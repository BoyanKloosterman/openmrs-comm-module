package nl.openmrs.comm_module.testgui.dto;

public record CancelAppointmentResultDto(
        boolean success,
        String appointmentFhirId,
        Integer openmrsAppointmentId,
        String message) {}
