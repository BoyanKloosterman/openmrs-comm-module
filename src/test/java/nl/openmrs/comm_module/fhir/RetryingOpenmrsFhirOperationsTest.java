package nl.openmrs.comm_module.fhir;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
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
        Bundle ok = new Bundle();
        when(delegate.searchEncountersSince("2026-01-01"))
                .thenThrow(tijdelijk("1"))
                .thenReturn(ok);

        assertEquals(ok, retrying.searchEncountersSince("2026-01-01"));
        verify(delegate, times(2)).searchEncountersSince("2026-01-01");
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
