package nl.openmrs.comm_module.poll.source;

import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.AppointmentFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.PatientFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r5.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** US-003: FHIR R5 Appointment + Patient ophalen en mappen. */
@Component
public class FhirR5AppointmentPollSource implements AppointmentPollSource {

    private static final Logger log = LoggerFactory.getLogger(FhirR5AppointmentPollSource.class);

    private final OpenmrsFhirOperations fhirOperations;
    private final AppointmentFhirMapper appointmentFhirMapper;
    private final PatientFhirMapper patientFhirMapper;

    public FhirR5AppointmentPollSource(
            OpenmrsFhirOperations fhirOperations,
            AppointmentFhirMapper appointmentFhirMapper,
            PatientFhirMapper patientFhirMapper) {
        this.fhirOperations = fhirOperations;
        this.appointmentFhirMapper = appointmentFhirMapper;
        this.patientFhirMapper = patientFhirMapper;
    }

    @Override
    public List<AppointmentWithPatientDto> fetchBetween(Instant from, Instant to) {
        List<Appointment> raw = fhirOperations.searchAppointmentsBetween(from, to);
        List<AppointmentPollDto> snapshots = mapAppointments(raw);
        return attachPatients(snapshots);
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

    private List<AppointmentWithPatientDto> attachPatients(List<AppointmentPollDto> appointments) {
        Map<String, Optional<PatientPollDto>> cache = new HashMap<>();
        List<AppointmentWithPatientDto> out = new ArrayList<>(appointments.size());
        for (AppointmentPollDto apt : appointments) {
            String pid = apt.patientId();
            Optional<PatientPollDto> patientOpt = cache.computeIfAbsent(pid, this::loadPatientPollDto);
            PatientPollDto patient = patientOpt.orElse(null);
            if (patient == null) {
                log.warn("Patient niet geladen voor appointment {} (patientId={})", apt.appointmentId(), pid);
            }
            out.add(new AppointmentWithPatientDto(apt, patient));
        }
        return out;
    }

    private Optional<PatientPollDto> loadPatientPollDto(String patientLogicalId) {
        return fhirOperations
                .readPatientByLogicalId(patientLogicalId)
                .flatMap(patientFhirMapper::mapPatient);
    }
}
