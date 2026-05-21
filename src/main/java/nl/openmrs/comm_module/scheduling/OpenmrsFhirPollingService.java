package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import nl.openmrs.comm_module.poll.source.AppointmentPollSource;
import nl.openmrs.comm_module.poll.source.AppointmentPollWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** US-003: periodieke poll-orchestratie (SRP: geen FHIR-mapping hier). */
@Component
public class OpenmrsFhirPollingService {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsFhirPollingService.class);

    private final OpenmrsFhirOperations fhirOperations;
    private final AppointmentPollSource appointmentPollSource;
    private final AppointmentPollPersistence appointmentPollPersistence;
    private final OpenmrsFhirProperties fhirProperties;

    public OpenmrsFhirPollingService(
            OpenmrsFhirOperations fhirOperations,
            AppointmentPollSource appointmentPollSource,
            AppointmentPollPersistence appointmentPollPersistence,
            OpenmrsFhirProperties fhirProperties) {
        this.fhirOperations = fhirOperations;
        this.appointmentPollSource = appointmentPollSource;
        this.appointmentPollPersistence = appointmentPollPersistence;
        this.fhirProperties = fhirProperties;
    }

    @Scheduled(fixedDelayString = "#{@openmrsFhirProperties.pollDelayMillis()}")
    public void pollOpenmrsFhir() {
        log.debug("OpenMRS FHIR poll gestart");
        try {
            String info = fhirOperations.fetchServerSoftwareNameAndVersion();
            log.info("FHIR server: {}", info);

            Instant now = Instant.now();
            Instant from = AppointmentPollWindow.from(now, fhirProperties);
            Instant to = AppointmentPollWindow.to(now);

            List<AppointmentWithPatientDto> withPatients =
                    appointmentPollSource.fetchBetween(from, to);

            long metPatient = withPatients.stream().filter(e -> e.patient() != null).count();
            log.info(
                    "Appointment-poll: {} rijen, {} met Patient (from={}, to={})",
                    withPatients.size(),
                    metPatient,
                    from,
                    to);

            appointmentPollPersistence.upsertPollResults(fhirProperties.getOrganisationId(), withPatients);
        } catch (RuntimeException e) {
            log.error(
                    "OpenMRS FHIR poll mislukt ({}): {}",
                    e.getClass().getSimpleName(),
                    shortMessage(e),
                    e);
        }
    }

    private static String shortMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) {
            return "geen detail";
        }
        String firstLine = msg.split("\\R", 2)[0];
        return firstLine.length() > 200 ? firstLine.substring(0, 200) + "..." : firstLine;
    }
}
