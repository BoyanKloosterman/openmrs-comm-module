package nl.openmrs.comm_module.sync;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** Leest SPA-afspraken uit patient_appointment (OpenMRS 3 reference distro). */
@Repository
@ConditionalOnBean(name = "openmrsJdbcTemplate")
public class OpenmrsPatientAppointmentJdbcRepository {

    private static final String SELECT_APPOINTMENTS =
            """
            SELECT
              pa.patient_appointment_id,
              trim(pa.uuid) AS appointment_uuid,
              upper(trim(pa.status)) AS status,
              coalesce(pa.voided, 0) AS voided,
              pa.date_changed,
              p.patient_id,
              trim(per.uuid) AS patient_uuid,
              pn.given_name,
              pn.family_name,
              pa.start_date_time AS start_date,
              pa.end_date_time AS end_date,
              trim(coalesce(ast.name, asvc.name, '')) AS type_name,
              trim(l.uuid) AS location_uuid,
              l.name AS location_name,
              trim(coalesce(pa.comments, '')) AS reason,
              (
                SELECT pa_attr.value
                FROM person_attribute pa_attr
                JOIN person_attribute_type pat
                  ON pa_attr.person_attribute_type_id = pat.person_attribute_type_id
                WHERE pa_attr.person_id = per.person_id
                  AND pa_attr.voided = false
                  AND pat.retired = false
                  AND (
                    lower(pat.name) LIKE '%phone%'
                    OR lower(pat.name) LIKE '%telefoon%'
                    OR lower(pat.name) LIKE '%mobile%'
                  )
                ORDER BY pa_attr.person_attribute_id
                LIMIT 1
              ) AS phone
            FROM patient_appointment pa
            JOIN patient p ON pa.patient_id = p.patient_id
            JOIN person per ON p.patient_id = per.person_id
            JOIN person_name pn
              ON per.person_id = pn.person_id AND pn.preferred = true AND pn.voided = false
            LEFT JOIN location l ON pa.location_id = l.location_id
            LEFT JOIN appointment_service_type ast
              ON pa.appointment_service_type_id = ast.appointment_service_type_id
            LEFT JOIN appointment_service asvc
              ON pa.appointment_service_id = asvc.appointment_service_id
            WHERE pa.start_date_time >= ? AND pa.start_date_time < ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public OpenmrsPatientAppointmentJdbcRepository(
            @Qualifier("openmrsJdbcTemplate") JdbcTemplate jdbcTemplate) {
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
                    rs.getInt("patient_appointment_id"),
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
