package nl.openmrs.comm_module.poll.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PolledEncounterRepository extends JpaRepository<PolledEncounterEntity, Long> {

    Optional<PolledEncounterEntity> findByOrganisationIdAndEncounterFhirId(String organisationId, String encounterFhirId);

    /** US-001-2: encounters in venster rond now + lead (nog niet begonnen, niet voided). */
    @Query("""
            SELECT e FROM PolledEncounterEntity e
            WHERE e.organisationId = :organisationId
              AND e.voided = false
              AND e.encounterDatetime > :now
              AND e.encounterDatetime >= :windowStart
              AND e.encounterDatetime < :windowEnd
            """)
    List<PolledEncounterEntity> findDueForReminderWindow(
            @Param("organisationId") String organisationId,
            @Param("now") Instant now,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);
}
