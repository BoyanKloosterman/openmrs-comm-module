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
    private final boolean jdbcFallbackEnabled;

    public OpenmrsPollOrganisationScope(
            OpenmrsFhirProperties fhirProperties,
            OpenmrsDataSourceProperties dataSourceProperties,
            @Value("${openmrs.fhir.poll-mode:fhir}") String pollMode,
            @Value("${openmrs.fhir.jdbc-fallback-enabled:true}") boolean jdbcFallbackEnabled) {
        this.fhirProperties = fhirProperties;
        this.dataSourceProperties = dataSourceProperties;
        this.pollMode = pollMode;
        this.jdbcFallbackEnabled = jdbcFallbackEnabled;
    }

    public List<String> activeOrganisationIds() {
        List<String> ids = new ArrayList<>();
        for (OrganisationFhirConnection connection : fhirProperties.resolveActiveConnections()) {
            ids.add(connection.organisationId());
        }
        if (ids.isEmpty() && jdbcPollActive()) {
            ids.add(fhirProperties.getOrganisationId());
        }
        return List.copyOf(ids);
    }

    public List<OrganisationFhirConnection> activePollConnections() {
        List<OrganisationFhirConnection> connections = fhirProperties.resolveActiveConnections();
        if (!connections.isEmpty()) {
            return connections;
        }
        if (jdbcPollActive()) {
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

    /** JDBC-only modus, of FHIR-modus met fallback en geconfigureerde MariaDB. */
    private boolean jdbcPollActive() {
        if (!dataSourceProperties.isConfigured()) {
            return false;
        }
        if ("jdbc".equalsIgnoreCase(pollMode)) {
            return true;
        }
        return "fhir".equalsIgnoreCase(pollMode) && jdbcFallbackEnabled;
    }
}
