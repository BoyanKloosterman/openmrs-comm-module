package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Instellingen voor OpenMRS Appointment Scheduling → FHIR R5 export. */
@Component("openmrsSchedulingSyncProperties")
@ConfigurationProperties(prefix = "openmrs.scheduling.sync")
public class OpenmrsSchedulingSyncProperties {

    private boolean enabled = true;

    private int intervalMinutes = 1;

    /** Interpretatie van naive timestamps uit OpenMRS (time_slot.start_date). */
    private String zoneId = "Europe/Amsterdam";

    private int lookbackDays = 30;

    private int lookaheadDays = 365;

    /** Alleen voor test/demo als patiënt geen telefoon-attribuut heeft. */
    private String fallbackPhone = "";

    public long delayMillis() {
        return Math.max(1, intervalMinutes) * 60L * 1000L;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public int getLookaheadDays() {
        return lookaheadDays;
    }

    public void setLookaheadDays(int lookaheadDays) {
        this.lookaheadDays = lookaheadDays;
    }

    public String getFallbackPhone() {
        return fallbackPhone;
    }

    public void setFallbackPhone(String fallbackPhone) {
        this.fallbackPhone = fallbackPhone;
    }
}
