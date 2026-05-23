package nl.openmrs.comm_module.fhir;

import ca.uhn.fhir.context.FhirContext;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** FHIR-client per organisatie; cache na eerste gebruik. */
@Component
public class OpenmrsFhirOperationsFactory {

    private final FhirContext fhirContext;
    private final OpenmrsFhirProperties properties;
    private final Map<String, OpenmrsFhirOperations> cache = new ConcurrentHashMap<>();

    public OpenmrsFhirOperationsFactory(
            @Qualifier("fhirContextR5") FhirContext fhirContext, OpenmrsFhirProperties properties) {
        this.fhirContext = fhirContext;
        this.properties = properties;
    }

    public List<OrganisationFhirConnection> activeConnections() {
        return properties.resolveActiveConnections();
    }

    public OpenmrsFhirOperations forOrganisation(String organisationId) {
        return cache.computeIfAbsent(organisationId, this::createOperations);
    }

    private OpenmrsFhirOperations createOperations(String organisationId) {
        OrganisationFhirConnection connection =
                properties.findConnection(organisationId).orElseThrow(
                        () -> new IllegalArgumentException("Geen FHIR-config voor organisatie: " + organisationId));
        OpenmrsFhirClient client = new OpenmrsFhirClient(fhirContext, connection);
        return new RetryingOpenmrsFhirOperations(client, properties);
    }
}
