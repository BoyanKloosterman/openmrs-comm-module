package nl.openmrs.comm_module.poll.source;

import nl.openmrs.comm_module.config.OpenmrsDataSourceProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.poll.PollDiagnosticsRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FallbackAppointmentPollSourceTest {

    @Mock
    private FhirR5AppointmentPollSource fhirPollSource;

    @Mock
    private JdbcPatientAppointmentPollSource jdbcPollSource;

    @Mock
    private PollDiagnosticsRecorder pollDiagnosticsRecorder;

    @Test
    void fhirSuccesGeenJdbc() {
        OpenmrsFhirProperties fhir = new OpenmrsFhirProperties();
        fhir.setOrganisationId("default");
        fhir.setServerUrl("http://localhost/fhir/R5");

        List<AppointmentWithPatientDto> expected =
                List.of(new AppointmentWithPatientDto(mock(AppointmentPollDto.class), null));
        when(fhirPollSource.fetchBetween(eq("default"), any(), any())).thenReturn(expected);

        FallbackAppointmentPollSource source = new FallbackAppointmentPollSource(
                fhirPollSource, emptyJdbcProvider(), fhir, new OpenmrsDataSourceProperties(), pollDiagnosticsRecorder, true);

        assertEquals(expected, source.fetchBetween("default", Instant.now(), Instant.now().plusSeconds(3600)));
    }

    @Test
    void fhirFoutGebruiktJdbc() {
        OpenmrsFhirProperties fhir = new OpenmrsFhirProperties();
        fhir.setOrganisationId("default");
        fhir.setServerUrl("http://localhost/fhir/R5");

        OpenmrsDataSourceProperties ds = new OpenmrsDataSourceProperties();
        ds.setUrl("jdbc:mariadb://localhost/openmrs");

        when(fhirPollSource.fetchBetween(any(), any(), any())).thenThrow(new RuntimeException("404"));
        List<AppointmentWithPatientDto> jdbcResult =
                List.of(new AppointmentWithPatientDto(mock(AppointmentPollDto.class), null));
        when(jdbcPollSource.fetchBetween(any(), any(), any())).thenReturn(jdbcResult);

        FallbackAppointmentPollSource source = new FallbackAppointmentPollSource(
                fhirPollSource, jdbcProvider(jdbcPollSource), fhir, ds, pollDiagnosticsRecorder, true);

        assertEquals(jdbcResult, source.fetchBetween("default", Instant.now(), Instant.now().plusSeconds(3600)));
        verify(jdbcPollSource).fetchBetween(eq("default"), any(), any());
    }

    @Test
    void fhirFoutZonderJdbcConfigured() {
        OpenmrsFhirProperties fhir = new OpenmrsFhirProperties();
        fhir.setOrganisationId("default");
        fhir.setServerUrl("http://localhost/fhir/R5");

        when(fhirPollSource.fetchBetween(any(), any(), any())).thenThrow(new RuntimeException("offline"));

        FallbackAppointmentPollSource source = new FallbackAppointmentPollSource(
                fhirPollSource, emptyJdbcProvider(), fhir, new OpenmrsDataSourceProperties(), pollDiagnosticsRecorder, true);

        assertThrows(IllegalStateException.class, () -> source.fetchBetween("default", Instant.now(), Instant.now()));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JdbcPatientAppointmentPollSource> jdbcProvider(JdbcPatientAppointmentPollSource jdbc) {
        ObjectProvider<JdbcPatientAppointmentPollSource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jdbc);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JdbcPatientAppointmentPollSource> emptyJdbcProvider() {
        ObjectProvider<JdbcPatientAppointmentPollSource> provider = mock(ObjectProvider.class);
        lenient().when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
