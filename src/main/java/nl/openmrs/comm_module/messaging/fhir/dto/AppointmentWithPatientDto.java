package nl.openmrs.comm_module.messaging.fhir.dto;

/** Appointment uit poll met optioneel opgehaalde Patient (FHIR read). */
public record AppointmentWithPatientDto(AppointmentPollDto appointment, PatientPollDto patient) {}
