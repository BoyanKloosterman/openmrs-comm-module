package nl.openmrs.comm_module.testgui.dto;

/** Resultaat bewerken/verwijderen OpenMRS-afspraak. */
public record MutateOpenmrsAppointmentResultDto(
        boolean success,
        int openmrsAppointmentId,
        String appointmentFhirId,
        String message) {}
