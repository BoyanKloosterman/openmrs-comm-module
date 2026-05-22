package nl.openmrs.comm_module.poll.source;

import nl.openmrs.comm_module.config.OpenmrsDataSourceProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.poll.PollDiagnosticsRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * FHIR R5 primair; bij fout, lege HAPI of geen FHIR-URL → JDBC (patient_appointment).
 */
@Component
public class FallbackAppointmentPollSource implements AppointmentPollSource {

    private static final Logger log = LoggerFactory.getLogger(FallbackAppointmentPollSource.class);

    private final FhirR5AppointmentPollSource fhirPollSource;
    private final ObjectProvider<JdbcPatientAppointmentPollSource> jdbcPollSource;
    private final OpenmrsFhirProperties fhirProperties;
    private final OpenmrsDataSourceProperties dataSourceProperties;
    private final PollDiagnosticsRecorder pollDiagnosticsRecorder;
    private final boolean jdbcFallbackEnabled;

    public FallbackAppointmentPollSource(
            FhirR5AppointmentPollSource fhirPollSource,
            ObjectProvider<JdbcPatientAppointmentPollSource> jdbcPollSource,
            OpenmrsFhirProperties fhirProperties,
            OpenmrsDataSourceProperties dataSourceProperties,
            PollDiagnosticsRecorder pollDiagnosticsRecorder,
            @Value("${openmrs.fhir.jdbc-fallback-enabled:true}") boolean jdbcFallbackEnabled) {
        this.fhirPollSource = fhirPollSource;
        this.jdbcPollSource = jdbcPollSource;
        this.fhirProperties = fhirProperties;
        this.dataSourceProperties = dataSourceProperties;
        this.pollDiagnosticsRecorder = pollDiagnosticsRecorder;
        this.jdbcFallbackEnabled = jdbcFallbackEnabled;
    }

    @Override
    public List<AppointmentWithPatientDto> fetchBetween(String organisationId, Instant from, Instant to) {
        if (fhirProperties.findConnection(organisationId).isPresent()) {
            try {
                return fhirPollSource.fetchBetween(organisationId, from, to);
            } catch (RuntimeException e) {
                log.warn(
                        "FHIR poll mislukt org={}, JDBC-fallback: {}",
                        organisationId,
                        e.getMessage());
                pollDiagnosticsRecorder.setError("FHIR: " + shortMessage(e));
                return jdbcFallback(organisationId, from, to);
            }
        }
        log.debug("Geen FHIR-URL voor org={}, JDBC-fallback", organisationId);
        return jdbcFallback(organisationId, from, to);
    }

    private List<AppointmentWithPatientDto> jdbcFallback(String organisationId, Instant from, Instant to) {
        if (!jdbcFallbackEnabled || !dataSourceProperties.isConfigured()) {
            throw new IllegalStateException(
                    "FHIR-bron niet beschikbaar en JDBC-fallback uit of niet geconfigureerd (org="
                            + organisationId
                            + ")");
        }
        JdbcPatientAppointmentPollSource jdbc = jdbcPollSource.getIfAvailable();
        if (jdbc == null) {
            throw new IllegalStateException(
                    "JDBC-fallback gevraagd maar OpenMRS-datasource niet actief (org=" + organisationId + ")");
        }
        pollDiagnosticsRecorder.setFhirServerInfo("JDBC fallback: " + dataSourceProperties.getUrl().trim());
        return jdbc.fetchBetween(organisationId, from, to);
    }

    private static String shortMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) {
            return t.getClass().getSimpleName();
        }
        String first = msg.split("\\R", 2)[0];
        return first.length() > 120 ? first.substring(0, 120) + "..." : first;
    }
}
