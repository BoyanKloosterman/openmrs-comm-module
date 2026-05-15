package nl.openmrs.comm_module.poll.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolledEncounterRepository extends JpaRepository<PolledEncounterEntity, Long> {

    Optional<PolledEncounterEntity> findByOrganisationIdAndEncounterFhirId(String organisationId, String encounterFhirId);
}
