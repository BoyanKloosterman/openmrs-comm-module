package nl.openmrs.comm_module.sync;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** Leest afspraken uit OpenMRS Appointment Scheduling-tabellen (zelfde Postgres als comm-module). */
@Repository
public class OpenmrsSchedulingJdbcRepository {

    private static final String SELECT_APPOINTMENTS =
            """
            SELECT
              a.appointment_id,
              trim(a.uuid) AS appointment_uuid,
              a.status,
              a.voided,
              a.date_changed,
              p.patient_id,
              trim(per.uuid) AS patient_uuid,
              pn.given_name,
              pn.family_name,
              ts.start_date,
              ts.end_date,
              at.name AS type_name,
              trim(l.uuid) AS location_uuid,
              l.name AS location_name,
              a.reason,
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
            FROM appointmentscheduling_appointment a
            JOIN patient p ON a.patient_id = p.patient_id
            JOIN person per ON p.patient_id = per.person_id
            JOIN person_name pn
              ON per.person_id = pn.person_id AND pn.preferred = true AND pn.voided = false
            JOIN appointmentscheduling_time_slot ts ON a.time_slot_id = ts.time_slot_id
            JOIN appointmentscheduling_appointment_block ab
              ON ts.appointment_block_id = ab.appointment_block_id
            JOIN location l ON ab.location_id = l.location_id
            JOIN appointmentscheduling_appointment_type at
              ON a.appointment_type_id = at.appointment_type_id
            WHERE ts.start_date >= ? AND ts.start_date < ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public OpenmrsSchedulingJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OpenmrsSchedulingAppointmentRow> findAppointmentsBetween(
            LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return jdbcTemplate.query(
                SELECT_APPOINTMENTS,
                ROW_MAPPER,
                Timestamp.valueOf(fromInclusive),
                Timestamp.valueOf(toExclusive));
    }

    private static final RowMapper<OpenmrsSchedulingAppointmentRow> ROW_MAPPER =
            (ResultSet rs, int rowNum) -> new OpenmrsSchedulingAppointmentRow(
                    rs.getInt("appointment_id"),
                    rs.getString("appointment_uuid"),
                    rs.getString("status"),
                    rs.getBoolean("voided"),
                    toLocalDateTime(rs.getTimestamp("date_changed")),
                    rs.getInt("patient_id"),
                    rs.getString("patient_uuid"),
                    rs.getString("given_name"),
                    rs.getString("family_name"),
                    toLocalDateTime(rs.getTimestamp("start_date")),
                    toLocalDateTime(rs.getTimestamp("end_date")),
                    rs.getString("type_name"),
                    rs.getString("location_uuid"),
                    rs.getString("location_name"),
                    rs.getString("reason"),
                    rs.getString("phone"));

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
