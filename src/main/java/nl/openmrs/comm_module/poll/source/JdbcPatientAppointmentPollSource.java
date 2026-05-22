package nl.openmrs.comm_module.poll.source;

import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.poll.PollDiagnosticsRecorder;
import nl.openmrs.comm_module.sync.OpenmrsPatientAppointmentJdbcRepository;
import nl.openmrs.comm_module.sync.OpenmrsSchedulingAppointmentRow;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Poll SPA-afspraken rechtstreeks uit OpenMRS MariaDB (geen FHIR2 Appointment op reference distro).
 */
@Component
@Primary
@ConditionalOnBean(OpenmrsPatientAppointmentJdbcRepository.class)
@ConditionalOnProperty(name = "openmrs.fhir.poll-mode", havingValue = "jdbc")
public class JdbcPatientAppointmentPollSource implements AppointmentPollSource {

    private final OpenmrsPatientAppointmentJdbcRepository appointmentRepository;
    private final OpenmrsSchedulingSyncProperties schedulingProperties;
    private final Clock clock;
    private final PollDiagnosticsRecorder pollDiagnosticsRecorder;

    public JdbcPatientAppointmentPollSource(
            OpenmrsPatientAppointmentJdbcRepository appointmentRepository,
            OpenmrsSchedulingSyncProperties schedulingProperties,
            Clock clock,
            PollDiagnosticsRecorder pollDiagnosticsRecorder) {
        this.appointmentRepository = appointmentRepository;
        this.schedulingProperties = schedulingProperties;
        this.clock = clock;
        this.pollDiagnosticsRecorder = pollDiagnosticsRecorder;
    }

    @Override
    public List<AppointmentWithPatientDto> fetchBetween(String organisationId, Instant from, Instant to) {
        ZoneId zone = ZoneId.of(schedulingProperties.getZoneId());
        LocalDateTime fromLocal = LocalDateTime.ofInstant(from, zone);
        LocalDateTime toLocal = LocalDateTime.ofInstant(to, zone);

        List<OpenmrsSchedulingAppointmentRow> rows =
                appointmentRepository.findAppointmentsBetween(fromLocal, toLocal);
        pollDiagnosticsRecorder.setFhirServerInfo("OpenMRS JDBC patient_appointment");
        pollDiagnosticsRecorder.setFhirRawCount(rows.size());

        Instant now = clock.instant();
        List<AppointmentWithPatientDto> out = new ArrayList<>();
        int mapped = 0;
        int skipped = 0;
        for (OpenmrsSchedulingAppointmentRow row : rows) {
            AppointmentPollDto apt = toPollDto(row, zone);
            if (apt == null) {
                skipped++;
                continue;
            }
            mapped++;
            PatientPollDto patient = toPatientDto(row, schedulingProperties);
            if (apt.appointmentDatetime() != null && !apt.appointmentDatetime().isAfter(now)) {
                out.add(new AppointmentWithPatientDto(apt, patient));
                continue;
            }
            out.add(new AppointmentWithPatientDto(apt, patient));
        }
        pollDiagnosticsRecorder.setMappedCounts(mapped, skipped);
        return out;
    }

    private static AppointmentPollDto toPollDto(OpenmrsSchedulingAppointmentRow row, ZoneId zone) {
        if (row.patientUuid() == null || row.patientUuid().isBlank()) {
            return null;
        }
        if (row.startDate() == null) {
            return null;
        }
        Instant start = row.startDate().atZone(zone).toInstant();
        boolean voided = row.voided() || isCancelledStatus(row.status());
        return new AppointmentPollDto(
                row.appointmentUuid(),
                row.fhirAppointmentId(),
                row.fhirPatientId(),
                start,
                row.locationName(),
                blankToNull(row.appointmentTypeName()),
                blankToNull(row.reason()),
                voided);
    }

    private static PatientPollDto toPatientDto(
            OpenmrsSchedulingAppointmentRow row, OpenmrsSchedulingSyncProperties properties) {
        String display = (trimOrEmpty(row.givenName()) + " " + trimOrEmpty(row.familyName())).trim();
        String phone = row.phone();
        if (phone == null || phone.isBlank()) {
            phone = properties.getFallbackPhone();
        }
        return new PatientPollDto(row.fhirPatientId(), display.isEmpty() ? null : display, blankToNull(phone));
    }

    private static boolean isCancelledStatus(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim().toUpperCase();
        return s.equals("CANCELLED") || s.equals("MISSED");
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
