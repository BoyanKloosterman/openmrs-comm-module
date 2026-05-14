package nl.openmrs.comm_module.fhir;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import org.springframework.stereotype.Component;
import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;

@Component
public class OpenmrsFhirClient {
    private final IGenericClient client;

    public OpenmrsFhirClient(@Qualifier("fhirContextR4") FhirContext fhirContext, @Value("${openmrs.fhir.server-url}") String fhirServerUrl, @Value("${openmrs.fhir.username}") String fhirUsername, @Value("${openmrs.fhir.password}") String fhirPassword) {
        this.client = fhirContext.newRestfulGenericClient(fhirServerUrl);
        this.client.registerInterceptor(new BasicAuthInterceptor(fhirUsername, fhirPassword));
    }
    
    public String fetchServerSoftwareNameAndVersion() {
        CapabilityStatement metadata = client
                .capabilities()
                .ofType(CapabilityStatement.class)
                .execute();
        return metadata.getSoftware().getName() + " " + metadata.getSoftware().getVersion();
    }

    public Bundle searchEncountersSince(String isoDate) {
        // isoDate bv. "2026-05-01" — FHIR date formaat (dag-resolutie)
        return client.search()
                .forResource(Encounter.class)
                .where(Encounter.DATE.afterOrEquals().day(isoDate))
                .returnBundle(Bundle.class)
                .execute();
    }
}