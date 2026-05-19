package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.AppointmentFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.PatientFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import org.hl7.fhir.r5.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Polling-service: appointments en patient via FHIR R5. */
@Component
public class OpenmrsFhirPollingService {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsFhirPollingService.class);

    // Bovenkant van het ophaalvenster; tickets verder weg dan dit hoeft de poller niet te zien.
    private static final long APPOINTMENT_WINDOW_DAYS_AHEAD = 365L;

    private final OpenmrsFhirOperations fhirOperations;
    private final AppointmentFhirMapper appointmentFhirMapper;
    private final PatientFhirMapper patientFhirMapper;
    private final AppointmentPollPersistence appointmentPollPersistence;
    private final OpenmrsFhirProperties fhirProperties;

    public OpenmrsFhirPollingService(
            OpenmrsFhirOperations fhirOperations,
            AppointmentFhirMapper appointmentFhirMapper,
            PatientFhirMapper patientFhirMapper,
            AppointmentPollPersistence appointmentPollPersistence,
            OpenmrsFhirProperties fhirProperties) {
        this.fhirOperations = fhirOperations;
        this.appointmentFhirMapper = appointmentFhirMapper;
        this.patientFhirMapper = patientFhirMapper;
        this.appointmentPollPersistence = appointmentPollPersistence;
        this.fhirProperties = fhirProperties;
    }

    @Scheduled(fixedDelayString = "#{@openmrsFhirProperties.pollDelayMillis()}")
    public void pollOpenmrsFhir() {
        log.debug("OpenMRS FHIR R5 poll gestart");
        try {
            String info = fhirOperations.fetchServerSoftwareNameAndVersion();
            log.info("FHIR server: {}", info);

            Instant now = Instant.now();
            Instant from = now.minus(Math.max(0, fhirProperties.getAppointmentPollSinceDays()), ChronoUnit.DAYS);
            Instant to = now.plus(APPOINTMENT_WINDOW_DAYS_AHEAD, ChronoUnit.DAYS);

            List<Appointment> raw = fhirOperations.searchAppointmentsBetween(from, to);
            List<AppointmentPollDto> snapshots = mapAppointments(raw);
            List<AppointmentWithPatientDto> withPatients = attachPatients(snapshots);

            long metPatient = withPatients.stream().filter(e -> e.patient() != null).count();
            log.info(
                    "Appointment-poll: {} FHIR-entries, {} appointments, {} met Patient (from={}, to={})",
                    raw.size(),
                    snapshots.size(),
                    metPatient,
                    from,
                    to);

            appointmentPollPersistence.upsertPollResults(fhirProperties.getOrganisationId(), withPatients);
        } catch (RuntimeException e) {
            log.error("OpenMRS FHIR poll mislukt ({}): {}",
                    e.getClass().getSimpleName(), shortMessage(e), e);
        }
    }

    private static String shortMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) {
            return "geen detail";
        }
        // Voorkomt HTML-blobs (zoals Tomcat 404-pages) in de log; eerste regel + max 200 tekens.
        String firstLine = msg.split("\\R", 2)[0];
        return firstLine.length() > 200 ? firstLine.substring(0, 200) + "..." : firstLine;
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
