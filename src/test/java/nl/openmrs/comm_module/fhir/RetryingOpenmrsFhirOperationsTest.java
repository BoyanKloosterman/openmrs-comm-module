package nl.openmrs.comm_module.fhir;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryingOpenmrsFhirOperationsTest {

    @Mock
    private OpenmrsFhirClient delegate;

    private OpenmrsFhirProperties properties;
    private RetryingOpenmrsFhirOperations retrying;

    @BeforeEach
    void setUp() {
        properties = new OpenmrsFhirProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setInitialBackoffMillis(0);
        properties.getRetry().setMultiplier(1.0);
        retrying = new RetryingOpenmrsFhirOperations(delegate, properties);
    }

    @Test
    void slaagtNaTijdelijkeFout() {
        Patient patient = new Patient();
        patient.setId("p1");
        Optional<Patient> ok = Optional.of(patient);
        when(delegate.readPatientByLogicalId("p1"))
                .thenThrow(tijdelijk("1"))
                .thenReturn(ok);

        assertEquals(ok, retrying.readPatientByLogicalId("p1"));
        verify(delegate, times(2)).readPatientByLogicalId("p1");
    }

    @Test
    void geeftDoorNaMaxPogingen() {
        when(delegate.fetchServerSoftwareNameAndVersion()).thenThrow(tijdelijk("x"));

        assertThrows(RuntimeException.class, retrying::fetchServerSoftwareNameAndVersion);
        verify(delegate, times(3)).fetchServerSoftwareNameAndVersion();
    }

    @Test
    void geenRetryOpNietTransientFout() {
        when(delegate.readPatientByLogicalId("1")).thenThrow(new IllegalArgumentException("bad"));

        assertThrows(IllegalArgumentException.class, () -> retrying.readPatientByLogicalId("1"));
        verify(delegate, times(1)).readPatientByLogicalId("1");
    }

    private static RuntimeException tijdelijk(String detail) {
        return new RuntimeException(detail, new IOException("tijdelijk"));
    }
}
