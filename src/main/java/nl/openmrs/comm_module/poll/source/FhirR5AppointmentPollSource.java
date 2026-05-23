package nl.openmrs.comm_module.poll.source;

import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperationsFactory;
import nl.openmrs.comm_module.messaging.fhir.AppointmentFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.PatientFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.poll.PollDiagnosticsRecorder;
import org.hl7.fhir.r5.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** US-003: FHIR R5 Appointment + Patient ophalen (via {@link FallbackAppointmentPollSource}). */
@Component
public class FhirR5AppointmentPollSource implements AppointmentPollSource {

    private static final Logger log = LoggerFactory.getLogger(FhirR5AppointmentPollSource.class);

    private final OpenmrsFhirOperationsFactory fhirOperationsFactory;
    private final AppointmentFhirMapper appointmentFhirMapper;
    private final PatientFhirMapper patientFhirMapper;
    private final Clock clock;
    private final PollDiagnosticsRecorder pollDiagnosticsRecorder;

    public FhirR5AppointmentPollSource(
            OpenmrsFhirOperationsFactory fhirOperationsFactory,
            AppointmentFhirMapper appointmentFhirMapper,
            PatientFhirMapper patientFhirMapper,
            Clock clock,
            PollDiagnosticsRecorder pollDiagnosticsRecorder) {
        this.fhirOperationsFactory = fhirOperationsFactory;
        this.appointmentFhirMapper = appointmentFhirMapper;
        this.patientFhirMapper = patientFhirMapper;
        this.clock = clock;
        this.pollDiagnosticsRecorder = pollDiagnosticsRecorder;
    }

    @Override
    public List<AppointmentWithPatientDto> fetchBetween(String organisationId, Instant from, Instant to) {
        OpenmrsFhirOperations fhirOperations = fhirOperationsFactory.forOrganisation(organisationId);
        List<Appointment> raw = fhirOperations.searchAppointmentsBetween(from, to);
        int rawCount = raw == null ? 0 : raw.size();
        pollDiagnosticsRecorder.setFhirRawCount(rawCount);
        List<AppointmentPollDto> snapshots = mapAppointments(raw);
        pollDiagnosticsRecorder.setMappedCounts(snapshots.size(), Math.max(0, rawCount - snapshots.size()));
        return attachPatients(organisationId, snapshots, fhirOperations);
    }

    private List<AppointmentPollDto> mapAppointments(List<Appointment> raw) {
        List<AppointmentPollDto> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (Appointment appointment : raw) {
            appointmentFhirMapper.map(appointment).ifPresent(out::add);
        }
        return out;
    }

    private List<AppointmentWithPatientDto> attachPatients(
            String organisationId, List<AppointmentPollDto> appointments, OpenmrsFhirOperations fhirOperations) {
        Instant now = clock.instant();
        Map<String, Optional<PatientPollDto>> cache = new HashMap<>();
        List<AppointmentWithPatientDto> out = new ArrayList<>(appointments.size());
        for (AppointmentPollDto apt : appointments) {
            if (!isUpcoming(apt, now)) {
                out.add(new AppointmentWithPatientDto(apt, null));
                continue;
            }
            String pid = apt.patientId();
            Optional<PatientPollDto> patientOpt =
                    cache.computeIfAbsent(pid, id -> loadPatientPollDto(fhirOperations, id));
            PatientPollDto patient = patientOpt.orElse(null);
            if (patient == null) {
                log.warn(
                        "Patient niet geladen voor appointment {} (org={}, patientId={})",
                        apt.appointmentId(),
                        organisationId,
                        pid);
            }
            out.add(new AppointmentWithPatientDto(apt, patient));
        }
        return out;
    }

    private Optional<PatientPollDto> loadPatientPollDto(OpenmrsFhirOperations fhirOperations, String patientLogicalId) {
        return fhirOperations.readPatientByLogicalId(patientLogicalId).flatMap(patientFhirMapper::mapPatient);
    }

    private static boolean isUpcoming(AppointmentPollDto appointment, Instant now) {
        Instant start = appointment.appointmentDatetime();
        return start != null && start.isAfter(now);
    }
}
