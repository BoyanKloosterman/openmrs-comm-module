package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** FHIR-instellingen; expliciete bean-naam voor @Scheduled SpEL. */
@Component("openmrsFhirProperties")
@ConfigurationProperties(prefix = "openmrs.fhir")
public class OpenmrsFhirProperties {

    /** Sleutel voor deze FHIR-bron (tenant); komt terug in polled_encounter.organisation_id. */
    private String organisationId = "default";

    /** Fallback als er geen override voor organisationId is. */
    private int pollIntervalMinutes = 1;

    private int encounterPollSinceDays = 30;

    /** Optioneel per organisatie: eigen poll-interval (US-003-8). */
    private Map<String, OrganisationPollSettings> organisations = new LinkedHashMap<>();

    private RetrySettings retry = new RetrySettings();

    public long pollDelayMillis() {
        int minutes = pollIntervalMinutes;
        OrganisationPollSettings override = organisations.get(organisationId);
        if (override != null && override.pollIntervalMinutes != null && override.pollIntervalMinutes > 0) {
            minutes = override.pollIntervalMinutes;
        }
        return minutes * 60L * 1000L;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public int getPollIntervalMinutes() {
        return pollIntervalMinutes;
    }

    public void setPollIntervalMinutes(int pollIntervalMinutes) {
        this.pollIntervalMinutes = pollIntervalMinutes;
    }

    public int getEncounterPollSinceDays() {
        return encounterPollSinceDays;
    }

    public void setEncounterPollSinceDays(int encounterPollSinceDays) {
        this.encounterPollSinceDays = encounterPollSinceDays;
    }

    public Map<String, OrganisationPollSettings> getOrganisations() {
        return organisations;
    }

    public void setOrganisations(Map<String, OrganisationPollSettings> organisations) {
        this.organisations = organisations != null ? organisations : new LinkedHashMap<>();
    }

    public RetrySettings getRetry() {
        return retry;
    }

    public void setRetry(RetrySettings retry) {
        this.retry = retry != null ? retry : new RetrySettings();
    }

    public static class OrganisationPollSettings {
        private Integer pollIntervalMinutes;

        public Integer getPollIntervalMinutes() {
            return pollIntervalMinutes;
        }

        public void setPollIntervalMinutes(Integer pollIntervalMinutes) {
            this.pollIntervalMinutes = pollIntervalMinutes;
        }
    }

    public static class RetrySettings {
        private int maxAttempts = 3;
        private long initialBackoffMillis = 500L;
        private double multiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialBackoffMillis() {
            return initialBackoffMillis;
        }

        public void setInitialBackoffMillis(long initialBackoffMillis) {
            this.initialBackoffMillis = initialBackoffMillis;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}
