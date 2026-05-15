package nl.openmrs.comm_module.poll.persistence;

import nl.openmrs.comm_module.messaging.fhir.dto.EncounterPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.poll.EncounterPollPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JpaEncounterPollPersistence implements EncounterPollPersistence {

    private final PolledEncounterRepository repository;

    public JpaEncounterPollPersistence(PolledEncounterRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void upsertPollResults(String organisationId, List<EncounterWithPatientDto> encountersWithPatients) {
        Instant now = Instant.now();
        for (EncounterWithPatientDto row : encountersWithPatients) {
            EncounterPollDto e = row.encounter();
            PolledEncounterEntity entity = repository
                    .findByOrganisationIdAndEncounterFhirId(organisationId, e.encounterId())
                    .orElseGet(PolledEncounterEntity::new);
            applyEncounter(entity, organisationId, e, row.patient(), now);
            repository.save(entity);
        }
    }

    private static void applyEncounter(
            PolledEncounterEntity entity,
            String organisationId,
            EncounterPollDto e,
            PatientPollDto patient,
            Instant lastPolledAt) {
        entity.setOrganisationId(organisationId);
        entity.setEncounterUuid(e.uuid());
        entity.setEncounterFhirId(e.encounterId());
        entity.setPatientFhirId(e.patientId());
        entity.setEncounterDatetime(e.encounterDatetime());
        entity.setLocationId(e.locationId());
        entity.setEncounterType(e.encounterType());
        entity.setVoided(e.voided());
        entity.setLastPolledAt(lastPolledAt);
        if (patient != null) {
            entity.setPatientDisplayName(patient.displayName());
            entity.setPatientPhone(patient.phone());
        } else {
            entity.setPatientDisplayName(null);
            entity.setPatientPhone(null);
        }
    }
}
