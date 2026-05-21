package nl.openmrs.comm_module.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.Patient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Ruwe HAPI FHIR R5-client voor één organisatie-bron. */
public class OpenmrsFhirClient implements OpenmrsFhirOperations {

    private final IGenericClient client;

    public OpenmrsFhirClient(FhirContext fhirContext, OrganisationFhirConnection connection) {
        this.client = fhirContext.newRestfulGenericClient(connection.serverUrl());
        if (connection.hasAuth()) {
            this.client.registerInterceptor(
                    new BasicAuthInterceptor(connection.username(), connection.password()));
        }
    }

    public String fetchServerSoftwareNameAndVersion() {
        CapabilityStatement metadata = client
                .capabilities()
                .ofType(CapabilityStatement.class)
                .execute();
        return metadata.getSoftware().getName() + " " + metadata.getSoftware().getVersion();
    }

    /** Haalt Appointment op id; leeg bij 404/410 of lege id. */
    public Optional<Appointment> readAppointmentByLogicalId(String logicalId) {
        if (logicalId == null || logicalId.isBlank()) {
            return Optional.empty();
        }
        try {
            Appointment appointment = client.read()
                    .resource(Appointment.class)
                    .withId(logicalId.trim())
                    .execute();
            return Optional.ofNullable(appointment);
        } catch (ResourceNotFoundException | ResourceGoneException e) {
            return Optional.empty();
        } catch (BaseServerResponseException e) {
            // 410 Gone: verwijderd in FHIR maar nog in polled_appointment
            if (e.getStatusCode() == 404 || e.getStatusCode() == 410) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /** Haalt Patient op id; leeg bij 404/410 of lege id. */
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
        } catch (ResourceNotFoundException | ResourceGoneException e) {
            return Optional.empty();
        } catch (BaseServerResponseException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 410) {
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

    @Override
    public void upsertPatient(Patient patient) {
        if (patient == null || !patient.hasId()) {
            throw new IllegalArgumentException("Patient zonder id");
        }
        client.update().resource(patient).execute();
    }

    @Override
    public void upsertAppointment(Appointment appointment) {
        if (appointment == null || !appointment.hasId()) {
            throw new IllegalArgumentException("Appointment zonder id");
        }
        client.update().resource(appointment).execute();
    }
}
