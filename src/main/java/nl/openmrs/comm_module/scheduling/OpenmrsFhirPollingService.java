package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperationsFactory;
import nl.openmrs.comm_module.fhir.OrganisationFhirConnection;
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

/** US-003: periodieke poll per gekoppelde FHIR-bron (SRP: geen FHIR-mapping hier). */
@Component
public class OpenmrsFhirPollingService {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsFhirPollingService.class);

    private final OpenmrsFhirOperationsFactory fhirOperationsFactory;
    private final AppointmentPollSource appointmentPollSource;
    private final AppointmentPollPersistence appointmentPollPersistence;
    private final OpenmrsFhirProperties fhirProperties;

    public OpenmrsFhirPollingService(
            OpenmrsFhirOperationsFactory fhirOperationsFactory,
            AppointmentPollSource appointmentPollSource,
            AppointmentPollPersistence appointmentPollPersistence,
            OpenmrsFhirProperties fhirProperties) {
        this.fhirOperationsFactory = fhirOperationsFactory;
        this.appointmentPollSource = appointmentPollSource;
        this.appointmentPollPersistence = appointmentPollPersistence;
        this.fhirProperties = fhirProperties;
    }

    @Scheduled(fixedDelayString = "#{@openmrsFhirProperties.pollDelayMillis()}")
    public void pollOpenmrsFhir() {
        log.debug("OpenMRS FHIR poll gestart");
        List<OrganisationFhirConnection> connections = fhirOperationsFactory.activeConnections();
        if (connections.isEmpty()) {
            log.warn("Geen actieve FHIR-bronnen geconfigureerd (openmrs.fhir.server-url of organisations.*)");
            return;
        }

        Instant now = Instant.now();
        Instant to = AppointmentPollWindow.to(now);

        for (OrganisationFhirConnection connection : connections) {
            pollOrganisation(connection, now, to);
        }
    }

    private void pollOrganisation(OrganisationFhirConnection connection, Instant now, Instant to) {
        String orgId = connection.organisationId();
        try {
            OpenmrsFhirOperations fhirOperations = fhirOperationsFactory.forOrganisation(orgId);
            String info = fhirOperations.fetchServerSoftwareNameAndVersion();
            log.info("FHIR server (org={}): {}", orgId, info);

            Instant from = AppointmentPollWindow.from(now, connection.appointmentPollSinceDays());
            List<AppointmentWithPatientDto> withPatients =
                    appointmentPollSource.fetchBetween(orgId, from, to);

            long metPatient = withPatients.stream().filter(e -> e.patient() != null).count();
            log.info(
                    "Appointment-poll org={}: {} rijen, {} met Patient (from={}, to={})",
                    orgId,
                    withPatients.size(),
                    metPatient,
                    from,
                    to);

            appointmentPollPersistence.upsertPollResults(orgId, withPatients);
        } catch (RuntimeException e) {
            log.error(
                    "OpenMRS FHIR poll mislukt org={} ({}): {}",
                    orgId,
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
