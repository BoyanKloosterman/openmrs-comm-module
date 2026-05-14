package nl.openmrs.comm_module.messaging.fhir.dto;

/** Minimale patient-snapshot voor koppeling aan encounter-poll (notificaties later). */
public record PatientPollDto(
        String patientId,
        String displayName,
        String phone
) {}
