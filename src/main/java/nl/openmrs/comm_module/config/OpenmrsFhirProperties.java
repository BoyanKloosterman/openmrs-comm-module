package nl.openmrs.comm_module.config;

import nl.openmrs.comm_module.fhir.OrganisationFhirConnection;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** FHIR-instellingen; expliciete bean-naam voor @Scheduled SpEL. */
@Component("openmrsFhirProperties")
@ConfigurationProperties(prefix = "openmrs.fhir")
public class OpenmrsFhirProperties {

    /** Sleutel voor standaard FHIR-bron (backward compatible). */
    private String organisationId = "default";

    private String serverUrl = "";
    private String username = "";
    private String password = "";

    private int pollIntervalMinutes = 1;
    private int appointmentPollSinceDays = 30;

    /** Per organisatie: eigen FHIR-server (US-003). */
    private Map<String, OrganisationFhirSettings> organisations = new LinkedHashMap<>();

    private RetrySettings retry = new RetrySettings();

    /** Scheduler-interval: kortste interval van alle actieve bronnen. */
    public long pollDelayMillis() {
        int minutes = pollIntervalMinutes;
        for (OrganisationFhirConnection connection : resolveActiveConnections()) {
            int orgMinutes = connection.pollIntervalMinutes();
            if (orgMinutes > 0) {
                minutes = Math.min(minutes, orgMinutes);
            }
        }
        return Math.max(1, minutes) * 60_000L;
    }

    /** Actieve FHIR-bronnen; lege organisations-map → één default-bron uit globale velden. */
    public List<OrganisationFhirConnection> resolveActiveConnections() {
        List<OrganisationFhirConnection> out = new ArrayList<>();
        for (Map.Entry<String, OrganisationFhirSettings> entry : organisations.entrySet()) {
            OrganisationFhirSettings settings = entry.getValue();
            if (settings == null || !settings.isActive()) {
                continue;
            }
            String url = settings.getServerUrl();
            if (url == null || url.isBlank()) {
                continue;
            }
            out.add(toConnection(entry.getKey(), settings, url));
        }
        if (out.isEmpty() && serverUrl != null && !serverUrl.isBlank()) {
            out.add(defaultConnection());
        }
        return List.copyOf(out);
    }

    public Optional<OrganisationFhirConnection> findConnection(String organisationId) {
        if (organisationId == null || organisationId.isBlank()) {
            return Optional.empty();
        }
        OrganisationFhirSettings settings = organisations.get(organisationId);
        if (settings != null && settings.isActive()) {
            String url = settings.getServerUrl();
            if (url != null && !url.isBlank()) {
                return Optional.of(toConnection(organisationId, settings, url));
            }
        }
        if (organisationId.equals(this.organisationId) && serverUrl != null && !serverUrl.isBlank()) {
            return Optional.of(defaultConnection());
        }
        return Optional.empty();
    }

    private OrganisationFhirConnection defaultConnection() {
        return new OrganisationFhirConnection(
                organisationId,
                serverUrl.trim(),
                username,
                password,
                pollIntervalMinutes,
                appointmentPollSinceDays);
    }

    private OrganisationFhirConnection toConnection(
            String orgId, OrganisationFhirSettings settings, String url) {
        int sinceDays = settings.getAppointmentPollSinceDays() != null
                ? settings.getAppointmentPollSinceDays()
                : appointmentPollSinceDays;
        int interval = settings.getPollIntervalMinutes() != null
                ? settings.getPollIntervalMinutes()
                : pollIntervalMinutes;
        String user = settings.getUsername() != null ? settings.getUsername() : username;
        String pass = settings.getPassword() != null ? settings.getPassword() : password;
        return new OrganisationFhirConnection(
                orgId, url.trim(), user, pass, Math.max(1, interval), Math.max(0, sinceDays));
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPollIntervalMinutes() {
        return pollIntervalMinutes;
    }

    public void setPollIntervalMinutes(int pollIntervalMinutes) {
        this.pollIntervalMinutes = pollIntervalMinutes;
    }

    public int getAppointmentPollSinceDays() {
        return appointmentPollSinceDays;
    }

    public void setAppointmentPollSinceDays(int appointmentPollSinceDays) {
        this.appointmentPollSinceDays = appointmentPollSinceDays;
    }

    public Map<String, OrganisationFhirSettings> getOrganisations() {
        return organisations;
    }

    public void setOrganisations(Map<String, OrganisationFhirSettings> organisations) {
        this.organisations = organisations != null ? organisations : new LinkedHashMap<>();
    }

    public RetrySettings getRetry() {
        return retry;
    }

    public void setRetry(RetrySettings retry) {
        this.retry = retry != null ? retry : new RetrySettings();
    }

    /** @deprecated gebruik {@link OrganisationFhirSettings}; alias voor bestaande properties-binding. */
    @Deprecated
    public OrganisationFhirSettings getOrganisationPollSettings(String organisationId) {
        return organisations.get(organisationId);
    }

    public static class OrganisationFhirSettings {
        private String serverUrl;
        private String username;
        private String password;
        private Integer pollIntervalMinutes;
        private Integer appointmentPollSinceDays;
        private boolean active = true;

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getPollIntervalMinutes() {
            return pollIntervalMinutes;
        }

        public void setPollIntervalMinutes(Integer pollIntervalMinutes) {
            this.pollIntervalMinutes = pollIntervalMinutes;
        }

        public Integer getAppointmentPollSinceDays() {
            return appointmentPollSinceDays;
        }

        public void setAppointmentPollSinceDays(Integer appointmentPollSinceDays) {
            this.appointmentPollSinceDays = appointmentPollSinceDays;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    /** Alias voor backward compatible tests. */
    public static class OrganisationPollSettings extends OrganisationFhirSettings {}

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
