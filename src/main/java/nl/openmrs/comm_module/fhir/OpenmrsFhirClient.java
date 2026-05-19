package nl.openmrs.comm_module.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class OpenmrsFhirClient implements OpenmrsFhirOperations {

    private final IGenericClient client;

    public OpenmrsFhirClient(
            @Qualifier("fhirContextR5") FhirContext fhirContext,
            @Value("${openmrs.fhir.server-url}") String fhirServerUrl,
            @Value("${openmrs.fhir.username}") String fhirUsername,
            @Value("${openmrs.fhir.password}") String fhirPassword) {
        this.client = fhirContext.newRestfulGenericClient(fhirServerUrl);
        // Alleen bij credentials (OpenMRS); standalone HAPI R5 in Docker heeft geen auth
        if (fhirUsername != null && !fhirUsername.isBlank()) {
            this.client.registerInterceptor(new BasicAuthInterceptor(fhirUsername, fhirPassword));
        }
    }

    public String fetchServerSoftwareNameAndVersion() {
        CapabilityStatement metadata = client
                .capabilities()
                .ofType(CapabilityStatement.class)
                .execute();
        return metadata.getSoftware().getName() + " " + metadata.getSoftware().getVersion();
    }

    /** Haalt Patient op id; leeg bij 404 of lege id. */
    public Optional<Patient> readPatientByLogicalId(String logicalId) {
        if (logicalId == null || logicalId.isBlank()) {
            return Optional.empty();
        }
        try {
            Patient patient = client.read()
                    .resource(Patient.class)
                    .withId(logicalId.trim())
                    .execute();
            return Optional.ofNullable(patient);
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        } catch (BaseServerResponseException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Zoekt appointments in het gegeven start-datumbereik (FHIR R5 search-parameter {@code date}).
     */
    public List<Appointment> searchAppointmentsBetween(Instant from, Instant to) {
        DateClientParam dateParam = Appointment.DATE;
        Bundle bundle = client.search()
                .forResource(Appointment.class)
                .where(dateParam.afterOrEquals().millis(Date.from(from)))
                .and(dateParam.beforeOrEquals().millis(Date.from(to)))
                .returnBundle(Bundle.class)
                .execute();
        return extractAppointments(bundle);
    }

    private List<Appointment> extractAppointments(Bundle bundle) {
        List<Appointment> appointments = new ArrayList<>();
        Bundle current = bundle;
        while (current != null) {
            for (Bundle.BundleEntryComponent entry : current.getEntry()) {
                if (entry.getResource() instanceof Appointment appointment) {
                    appointments.add(appointment);
                }
            }
            current = loadNextPage(current);
        }
        return appointments;
    }

    private Bundle loadNextPage(Bundle bundle) {
        if (bundle == null || bundle.getLink(IBaseBundle.LINK_NEXT) == null) {
            return null;
        }
        return client.loadPage().next(bundle).execute();
    }
}
