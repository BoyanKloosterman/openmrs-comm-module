package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsDataSourceProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.fhir.OrganisationFhirConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Welke organisatie-ids poll en herinneringsquery gebruiken (FHIR-URL of JDBC-fallback). */
@Component
public class OpenmrsPollOrganisationScope {

    private final OpenmrsFhirProperties fhirProperties;
    private final OpenmrsDataSourceProperties dataSourceProperties;
    private final String pollMode;

    public OpenmrsPollOrganisationScope(
            OpenmrsFhirProperties fhirProperties,
            OpenmrsDataSourceProperties dataSourceProperties,
            @Value("${openmrs.fhir.poll-mode:fhir}") String pollMode) {
        this.fhirProperties = fhirProperties;
        this.dataSourceProperties = dataSourceProperties;
        this.pollMode = pollMode;
    }

    public List<String> activeOrganisationIds() {
        List<String> ids = new ArrayList<>();
        for (OrganisationFhirConnection connection : fhirProperties.resolveActiveConnections()) {
            ids.add(connection.organisationId());
        }
        if (ids.isEmpty() && jdbcPollFallback()) {
            ids.add(fhirProperties.getOrganisationId());
        }
        return List.copyOf(ids);
    }

    public List<OrganisationFhirConnection> activePollConnections() {
        List<OrganisationFhirConnection> connections = fhirProperties.resolveActiveConnections();
        if (!connections.isEmpty()) {
            return connections;
        }
        if (jdbcPollFallback()) {
            return List.of(jdbcConnection());
        }
        return List.of();
    }

    private OrganisationFhirConnection jdbcConnection() {
        return new OrganisationFhirConnection(
                fhirProperties.getOrganisationId(),
                "jdbc:openmrs",
                "",
                "",
                fhirProperties.getPollIntervalMinutes(),
                fhirProperties.getAppointmentPollSinceDays());
    }

    private boolean jdbcPollFallback() {
        return "jdbc".equalsIgnoreCase(pollMode) && dataSourceProperties.isConfigured();
    }
}
