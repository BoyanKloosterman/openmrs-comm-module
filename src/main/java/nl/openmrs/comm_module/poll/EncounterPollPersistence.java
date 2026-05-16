package nl.openmrs.comm_module.poll;

import nl.openmrs.comm_module.messaging.fhir.dto.EncounterWithPatientDto;

import java.util.List;

/** Poort: poll-resultaat naar persistentie (JPA kan later worden gewisseld). */
public interface EncounterPollPersistence {

    void upsertPollResults(String organisationId, List<EncounterWithPatientDto> encountersWithPatients);
}
