package nl.openmrs.comm_module.poll.source;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Berekent het FHIR-zoekvenster (SRP: alleen tijdsinterval). */
public final class AppointmentPollWindow {

    private static final long DAYS_AHEAD = 365L;

    private AppointmentPollWindow() {}

    public static Instant from(Instant now, OpenmrsFhirProperties properties) {
        return from(now, properties.getAppointmentPollSinceDays());
    }

    public static Instant from(Instant now, int appointmentPollSinceDays) {
        return now.minus(Math.max(0, appointmentPollSinceDays), ChronoUnit.DAYS);
    }

    public static Instant to(Instant now) {
        return now.plus(DAYS_AHEAD, ChronoUnit.DAYS);
    }
}
