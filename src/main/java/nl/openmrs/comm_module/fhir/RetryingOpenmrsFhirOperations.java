package nl.openmrs.comm_module.fhir;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Retry rond ruwe {@link OpenmrsFhirClient} (US-003-7); alleen tijdelijke fouten.
 */
@Component
@Primary
public class RetryingOpenmrsFhirOperations implements OpenmrsFhirOperations {

    private static final Logger log = LoggerFactory.getLogger(RetryingOpenmrsFhirOperations.class);

    private final OpenmrsFhirClient delegate;
    private final OpenmrsFhirProperties properties;

    public RetryingOpenmrsFhirOperations(OpenmrsFhirClient delegate, OpenmrsFhirProperties properties) {
        this.delegate = delegate;
        this.properties = properties;
    }

    @Override
    public String fetchServerSoftwareNameAndVersion() {
        return executeWithRetry("capabilities", delegate::fetchServerSoftwareNameAndVersion);
    }

    @Override
    public Optional<Patient> readPatientByLogicalId(String logicalId) {
        return executeWithRetry("read Patient", () -> delegate.readPatientByLogicalId(logicalId));
    }

    @Override
    public List<Appointment> searchAppointmentsBetween(Instant from, Instant to) {
        return executeWithRetry("search Appointment", () -> delegate.searchAppointmentsBetween(from, to));
    }

    @Override
    public void upsertPatient(Patient patient) {
        executeWithRetry("upsert Patient", () -> {
            delegate.upsertPatient(patient);
            return null;
        });
    }

    @Override
    public void upsertAppointment(Appointment appointment) {
        executeWithRetry("upsert Appointment", () -> {
            delegate.upsertAppointment(appointment);
            return null;
        });
    }

    @FunctionalInterface
    private interface FhirCall<T> {
        T run();
    }

    private <T> T executeWithRetry(String label, FhirCall<T> call) {
        OpenmrsFhirProperties.RetrySettings r = properties.getRetry();
        int max = Math.max(1, r.getMaxAttempts());
        long backoff = Math.max(0L, r.getInitialBackoffMillis());
        double mult = r.getMultiplier() > 1.0 ? r.getMultiplier() : 1.0;
        RuntimeException last = null;
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                return call.run();
            } catch (RuntimeException e) {
                last = e;
                if (attempt >= max || !isTransient(e)) {
                    throw e;
                }
                log.warn("FHIR {} mislukt (poging {}/{}), retry na {} ms: {}", label, attempt, max, backoff, e.toString());
                sleepQuietly(backoff);
                backoff = (long) (backoff * mult);
            }
        }
        throw last != null ? last : new IllegalStateException("retry zonder resultaat");
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FHIR retry onderbroken", ie);
        }
    }

    private static boolean isTransient(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IOException && !(cur instanceof FileNotFoundException)) {
                return true;
            }
            if (cur instanceof BaseServerResponseException bse) {
                int code = bse.getStatusCode();
                return code == 408 || code >= 500;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
