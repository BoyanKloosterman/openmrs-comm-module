package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperationsFactory;
import nl.openmrs.comm_module.fhir.OrganisationFhirConnection;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import nl.openmrs.comm_module.poll.PollDiagnosticsRecorder;
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
    private final PollDiagnosticsRecorder pollDiagnosticsRecorder;
    private final OpenmrsPollOrganisationScope pollOrganisationScope;

    public OpenmrsFhirPollingService(
            OpenmrsFhirOperationsFactory fhirOperationsFactory,
            AppointmentPollSource appointmentPollSource,
            AppointmentPollPersistence appointmentPollPersistence,
            PollDiagnosticsRecorder pollDiagnosticsRecorder,
            OpenmrsPollOrganisationScope pollOrganisationScope) {
        this.fhirOperationsFactory = fhirOperationsFactory;
        this.appointmentPollSource = appointmentPollSource;
        this.appointmentPollPersistence = appointmentPollPersistence;
        this.pollDiagnosticsRecorder = pollDiagnosticsRecorder;
        this.pollOrganisationScope = pollOrganisationScope;
    }

    @Scheduled(fixedDelayString = "#{@openmrsFhirProperties.pollDelayMillis()}")
    public void pollOpenmrsFhir() {
        log.debug("OpenMRS FHIR poll gestart");
        List<OrganisationFhirConnection> connections = pollOrganisationScope.activePollConnections();
        Instant now = Instant.now();
        Instant to = AppointmentPollWindow.to(now);

        if (connections.isEmpty()) {
            log.warn("Geen actieve FHIR-bronnen geconfigureerd (openmrs.fhir.server-url of organisations.*)");
            return;
        }

        for (OrganisationFhirConnection connection : connections) {
            pollOrganisation(connection, now, to);
        }
    }

    private void pollOrganisation(OrganisationFhirConnection connection, Instant now, Instant to) {
        String orgId = connection.organisationId();
        Instant from = AppointmentPollWindow.from(now, connection.appointmentPollSinceDays());
        String pollLabel = resolvePollLabel(connection);
        pollDiagnosticsRecorder.begin(orgId, pollLabel, from, to);
        try {
            if (usesFhirServer(connection)) {
                probeFhirServer(orgId);
            }
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
            String msg = shortMessage(e);
            pollDiagnosticsRecorder.setError(msg);
            log.warn(
                    "Appointment-poll mislukt org={} ({}): {}",
                    orgId,
                    e.getClass().getSimpleName(),
                    msg);
        }
    }

    /** Optioneel metadata; faalt niet de poll (JDBC-fallback volgt in AppointmentPollSource). */
    private void probeFhirServer(String orgId) {
        try {
            OpenmrsFhirOperations fhirOperations = fhirOperationsFactory.forOrganisation(orgId);
            String info = fhirOperations.fetchServerSoftwareNameAndVersion();
            pollDiagnosticsRecorder.setFhirServerInfo(info);
            log.info("FHIR server (org={}): {}", orgId, info);
        } catch (RuntimeException e) {
            log.debug("FHIR metadata niet beschikbaar org={}: {}", orgId, shortMessage(e));
            pollDiagnosticsRecorder.setFhirServerInfo(
                    "FHIR metadata niet beschikbaar (controleer OPENMRS_FHIR_SERVER_URL, bijv. HAPI R5)");
        }
    }

    private static boolean usesFhirServer(OrganisationFhirConnection connection) {
        String url = connection.serverUrl();
        return url != null && !url.isBlank() && !url.startsWith("jdbc:");
    }

    private static String resolvePollLabel(OrganisationFhirConnection connection) {
        return connection.serverUrl();
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
