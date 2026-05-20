package nl.openmrs.comm_module.testgui;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Test-only JDBC: patiënten uit OpenMRS, boeken en annuleren in appointmentscheduling_*. */
@Repository
public class OpenmrsSchedulingTestRepository {

    private static final int CREATOR_USER_ID = 1;

    private static final String SELECT_PATIENTS =
            """
            SELECT
              p.patient_id,
              trim(per.uuid) AS patient_uuid,
              trim(coalesce(pn.given_name, '')) AS given_name,
              trim(coalesce(pn.family_name, '')) AS family_name,
              (
                SELECT pa.value
                FROM person_attribute pa
                JOIN person_attribute_type pat
                  ON pa.person_attribute_type_id = pat.person_attribute_type_id
                WHERE pa.person_id = per.person_id
                  AND pa.voided = false
                  AND pat.retired = false
                  AND (
                    lower(pat.name) LIKE '%phone%'
                    OR lower(pat.name) LIKE '%telefoon%'
                    OR lower(pat.name) LIKE '%mobile%'
                  )
                ORDER BY pa.person_attribute_id
                LIMIT 1
              ) AS phone
            FROM patient p
            JOIN person per ON p.patient_id = per.person_id
            JOIN person_name pn
              ON per.person_id = pn.person_id AND pn.preferred = true AND pn.voided = false
            WHERE per.voided = false
            ORDER BY pn.family_name, pn.given_name
            LIMIT ?
            """;

    private static final String SELECT_PATIENT_BY_UUID =
            """
            SELECT
              p.patient_id,
              trim(per.uuid) AS patient_uuid,
              trim(coalesce(pn.given_name, '')) AS given_name,
              trim(coalesce(pn.family_name, '')) AS family_name,
              (
                SELECT pa.value
                FROM person_attribute pa
                JOIN person_attribute_type pat
                  ON pa.person_attribute_type_id = pat.person_attribute_type_id
                WHERE pa.person_id = per.person_id
                  AND pa.voided = false
                  AND pat.retired = false
                  AND (
                    lower(pat.name) LIKE '%phone%'
                    OR lower(pat.name) LIKE '%telefoon%'
                    OR lower(pat.name) LIKE '%mobile%'
                  )
                ORDER BY pa.person_attribute_id
                LIMIT 1
              ) AS phone
            FROM patient p
            JOIN person per ON p.patient_id = per.person_id
            JOIN person_name pn
              ON per.person_id = pn.person_id AND pn.preferred = true AND pn.voided = false
            WHERE per.voided = false
              AND trim(per.uuid) = ?
            """;

    private static final String SELECT_LOCATIONS =
            """
            SELECT location_id, trim(uuid) AS location_uuid, trim(name) AS name
            FROM location
            WHERE retired = false
            ORDER BY name
            LIMIT ?
            """;

    private static final String SELECT_LOCATION_BY_UUID =
            """
            SELECT location_id, trim(uuid) AS location_uuid, trim(name) AS name
            FROM location
            WHERE retired = false AND trim(uuid) = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public OpenmrsSchedulingTestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OpenmrsLocationRow> listLocations(int limit) {
        return jdbcTemplate.query(SELECT_LOCATIONS, LOCATION_ROW_MAPPER, Math.max(1, limit));
    }

    public Optional<OpenmrsLocationRow> findLocationByUuid(String locationUuid) {
        if (locationUuid == null || locationUuid.isBlank()) {
            return Optional.empty();
        }
        List<OpenmrsLocationRow> rows =
                jdbcTemplate.query(SELECT_LOCATION_BY_UUID, LOCATION_ROW_MAPPER, locationUuid.trim());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<OpenmrsPatientRow> listPatients(int limit) {
        return jdbcTemplate.query(SELECT_PATIENTS, PATIENT_ROW_MAPPER, Math.max(1, limit));
    }

    public Optional<OpenmrsPatientRow> findPatientByUuid(String patientUuid) {
        if (patientUuid == null || patientUuid.isBlank()) {
            return Optional.empty();
        }
        List<OpenmrsPatientRow> rows =
                jdbcTemplate.query(SELECT_PATIENT_BY_UUID, PATIENT_ROW_MAPPER, patientUuid.trim());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public BookedOpenmrsAppointment bookAppointment(
            String patientUuid, String locationUuid, LocalDateTime start, String reason) {
        OpenmrsPatientRow patient =
                findPatientByUuid(patientUuid)
                        .orElseThrow(() -> new IllegalArgumentException("Patiënt niet gevonden in OpenMRS: " + patientUuid));
        OpenmrsLocationRow location =
                findLocationByUuid(locationUuid)
                        .orElseThrow(() -> new IllegalArgumentException("Locatie niet gevonden in OpenMRS: " + locationUuid));

        LocalDateTime end = start.plusHours(1);
        int timeSlotId = findOrCreateTimeSlot(location.locationId(), start, end);
        int appointmentTypeId = resolveAppointmentTypeId();
        String trimmedReason = reason == null ? "" : reason.trim();

        int appointmentId =
                insertReturningId(
                        """
                        INSERT INTO appointmentscheduling_appointment (
                          time_slot_id, patient_id, appointment_type_id,
                          status, reason, uuid, creator, date_created, voided
                        ) VALUES (?, ?, ?, 'SCHEDULED', ?, ?, ?, NOW(), false)
                        """,
                        "appointment_id",
                        ps -> {
                            ps.setInt(1, timeSlotId);
                            ps.setInt(2, patient.patientId());
                            ps.setInt(3, appointmentTypeId);
                            ps.setString(4, trimmedReason);
                            ps.setString(5, UUID.randomUUID().toString());
                            ps.setInt(6, CREATOR_USER_ID);
                        });
        return new BookedOpenmrsAppointment(
                appointmentId,
                "omrs-appt-" + appointmentId,
                "omrs-patient-" + patient.patientUuid(),
                patient.displayName(),
                location.name(),
                start,
                trimmedReason);
    }

    public boolean cancelAppointment(int openmrsAppointmentId) {
        int updated =
                jdbcTemplate.update(
                        """
                        UPDATE appointmentscheduling_appointment
                        SET status = 'CANCELLED',
                            date_changed = NOW(),
                            changed_by = ?
                        WHERE appointment_id = ?
                          AND voided = false
                          AND upper(status) NOT IN ('CANCELLED', 'MISSED')
                        """,
                        CREATOR_USER_ID,
                        openmrsAppointmentId);
        return updated > 0;
    }

    public Optional<Integer> resolveOpenmrsAppointmentId(String appointmentFhirId) {
        if (appointmentFhirId == null || !appointmentFhirId.startsWith("omrs-appt-")) {
            return Optional.empty();
        }
        String suffix = appointmentFhirId.substring("omrs-appt-".length());
        try {
            return Optional.of(Integer.parseInt(suffix));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private int findOrCreateTimeSlot(int locationId, LocalDateTime start, LocalDateTime end) {
        Optional<Integer> existing =
                jdbcTemplate
                        .query(
                                """
                                SELECT ts.time_slot_id
                                FROM appointmentscheduling_time_slot ts
                                JOIN appointmentscheduling_appointment_block ab
                                  ON ts.appointment_block_id = ab.appointment_block_id
                                WHERE ab.location_id = ?
                                  AND ts.voided = false
                                  AND ab.voided = false
                                  AND ts.start_date = ?
                                  AND NOT EXISTS (
                                    SELECT 1 FROM appointmentscheduling_appointment a
                                    WHERE a.time_slot_id = ts.time_slot_id
                                      AND a.voided = false
                                      AND upper(a.status) NOT IN ('CANCELLED', 'MISSED')
                                  )
                                LIMIT 1
                                """,
                                (rs, rowNum) -> rs.getInt(1),
                                locationId,
                                Timestamp.valueOf(start))
                        .stream()
                        .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        int blockId = resolveAppointmentBlockId(locationId);
        return insertReturningId(
                """
                INSERT INTO appointmentscheduling_time_slot (
                  appointment_block_id, start_date, end_date,
                  uuid, creator, date_created, voided
                ) VALUES (?, ?, ?, ?, ?, NOW(), false)
                """,
                "time_slot_id",
                ps -> {
                    ps.setInt(1, blockId);
                    ps.setTimestamp(2, Timestamp.valueOf(start));
                    ps.setTimestamp(3, Timestamp.valueOf(end));
                    ps.setString(4, UUID.randomUUID().toString());
                    ps.setInt(5, CREATOR_USER_ID);
                });
    }

    private int insertReturningId(String sql, String idColumn, StatementBinder binder) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, new String[] {idColumn});
                    binder.bind(ps);
                    return ps;
                },
                keys);
        Number id = keys.getKeyAs(Number.class);
        if (id == null) {
            throw new IllegalStateException("Geen " + idColumn + " na insert");
        }
        return id.intValue();
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws java.sql.SQLException;
    }

    private int resolveAppointmentBlockId(int locationId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT ab.appointment_block_id
                    FROM appointmentscheduling_appointment_block ab
                    WHERE ab.voided = false AND ab.location_id = ?
                    ORDER BY ab.appointment_block_id DESC
                    LIMIT 1
                    """,
                    Integer.class,
                    locationId);
        } catch (EmptyResultDataAccessException e) {
            return createAppointmentBlock(locationId);
        }
    }

    /** Blok voor locatie + demo-provider (zelfde patroon als seed-appointments.ps1). */
    private int createAppointmentBlock(int locationId) {
        int providerId = resolveProviderId();
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(1);
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(90);
        return insertReturningId(
                """
                INSERT INTO appointmentscheduling_appointment_block (
                  location_id, provider_id, start_date, end_date,
                  uuid, creator, date_created, voided
                ) VALUES (?, ?, ?, ?, ?, ?, NOW(), false)
                """,
                "appointment_block_id",
                ps -> {
                    ps.setInt(1, locationId);
                    ps.setInt(2, providerId);
                    ps.setTimestamp(3, Timestamp.valueOf(rangeStart));
                    ps.setTimestamp(4, Timestamp.valueOf(rangeEnd));
                    ps.setString(5, UUID.randomUUID().toString());
                    ps.setInt(6, CREATOR_USER_ID);
                });
    }

    private int resolveProviderId() {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT provider_id FROM provider
                    WHERE identifier = 'demo-provider'
                    LIMIT 1
                    """,
                    Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return jdbcTemplate.queryForObject(
                    "SELECT provider_id FROM provider ORDER BY provider_id LIMIT 1", Integer.class);
        }
    }

    private int resolveAppointmentTypeId() {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT appointment_type_id
                    FROM appointmentscheduling_appointment_type
                    WHERE retired = false
                    ORDER BY appointment_type_id
                    LIMIT 1
                    """,
                    Integer.class);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException(
                    "Geen appointment type in OpenMRS; seed eerst demo-rooster (seed-appointments.ps1)");
        }
    }

    private static final RowMapper<OpenmrsLocationRow> LOCATION_ROW_MAPPER =
            (rs, rowNum) ->
                    new OpenmrsLocationRow(
                            rs.getInt("location_id"),
                            rs.getString("location_uuid"),
                            rs.getString("name"));

    private static final RowMapper<OpenmrsPatientRow> PATIENT_ROW_MAPPER =
            (rs, rowNum) ->
                    new OpenmrsPatientRow(
                            rs.getInt("patient_id"),
                            rs.getString("patient_uuid"),
                            rs.getString("given_name"),
                            rs.getString("family_name"),
                            rs.getString("phone"));

    public record OpenmrsPatientRow(
            int patientId, String patientUuid, String givenName, String familyName, String phone) {

        public String displayName() {
            String g = givenName == null ? "" : givenName.trim();
            String f = familyName == null ? "" : familyName.trim();
            if (!g.isEmpty() && !f.isEmpty()) {
                return g + " " + f;
            }
            if (!f.isEmpty()) {
                return f;
            }
            return g.isEmpty() ? "Onbekend" : g;
        }
    }

    public record OpenmrsLocationRow(int locationId, String locationUuid, String name) {}

    public record BookedOpenmrsAppointment(
            int openmrsAppointmentId,
            String fhirAppointmentId,
            String fhirPatientId,
            String patientDisplayName,
            String locationName,
            LocalDateTime start,
            String reason) {}
}
