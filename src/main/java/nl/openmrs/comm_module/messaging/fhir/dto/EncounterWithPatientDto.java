package nl.openmrs.comm_module.messaging.fhir.dto;

/**
 * Encounter uit poll met optioneel opgehaalde Patient (FHIR read); patient kan null zijn bij 404/parse-fout.
 */
public record EncounterWithPatientDto(EncounterPollDto encounter, PatientPollDto patient) {}
