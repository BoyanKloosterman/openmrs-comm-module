package nl.openmrs.comm_module.poll.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PolledAppointmentRepository extends JpaRepository<PolledAppointmentEntity, Long> {

    Optional<PolledAppointmentEntity> findByOrganisationIdAndAppointmentFhirId(
            String organisationId, String appointmentFhirId);

    @Query("""
            SELECT a FROM PolledAppointmentEntity a
            WHERE a.organisationId = :organisationId
              AND a.voided = false
              AND a.appointmentDatetime > :now
              AND a.appointmentDatetime >= :windowStart
              AND a.appointmentDatetime < :windowEnd
            """)
    List<PolledAppointmentEntity> findDueForReminderWindow(
            @Param("organisationId") String organisationId,
            @Param("now") Instant now,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);

    List<PolledAppointmentEntity> findByAppointmentDatetimeBefore(Instant cutoff);

    @Modifying
    @Query("""
            DELETE FROM PolledAppointmentEntity a
            WHERE a.appointmentDatetime < :cutoff
            """)
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
    /** Rijen die nog in DB staan maar niet meer in de toekomst liggen (US-003). */
    List<PolledAppointmentEntity> findByOrganisationIdAndAppointmentDatetimeLessThanEqual(
            String organisationId, Instant cutoff);
}
