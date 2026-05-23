package nl.openmrs.comm_module.config;

import nl.openmrs.comm_module.poll.source.AppointmentPollSource;
import nl.openmrs.comm_module.poll.source.FallbackAppointmentPollSource;
import nl.openmrs.comm_module.poll.source.JdbcPatientAppointmentPollSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Kiest primaire poll-bron: FHIR (+ JDBC-fallback) of alleen JDBC. */
@Configuration
public class AppointmentPollSourceConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "openmrs.fhir.poll-mode", havingValue = "fhir", matchIfMissing = true)
    AppointmentPollSource primaryFhirPollSource(FallbackAppointmentPollSource fallback) {
        return fallback;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "openmrs.fhir.poll-mode", havingValue = "jdbc")
    AppointmentPollSource primaryJdbcPollSource(JdbcPatientAppointmentPollSource jdbc) {
        return jdbc;
    }
}
