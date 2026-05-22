package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/** Instellingen voor OpenMRS Appointment Scheduling → FHIR R5 export. */
@Component("openmrsSchedulingSyncProperties")
@ConfigurationProperties(prefix = "openmrs.scheduling.sync")
public class OpenmrsSchedulingSyncProperties {

    private boolean enabled = true;

    /** legacy = appointmentscheduling_* ; patient-appointment = SPA reference distro. */
    private String source = "legacy";

    private int intervalMinutes = 1;

    /** Weergave/herinneringen: Instant → lokale tijd in notificaties. */
    private String zoneId = "Europe/Amsterdam";

    /**
     * Zone van naive datetimes in de OpenMRS DB.
     * Reference distro SPA (patient_appointment): meestal UTC — 11:03 in DB = 13:03 in NL (CEST).
     */
    private String dbZoneId = "UTC";

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source != null ? source.trim() : "legacy";
    }

    public boolean isPatientAppointmentSource() {
        return "patient-appointment".equalsIgnoreCase(source);
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

    public String getDbZoneId() {
        return dbZoneId;
    }

    public void setDbZoneId(String dbZoneId) {
        this.dbZoneId = dbZoneId;
    }

    /** Zone om start_date_time uit JDBC te lezen (naive kolom in DB). */
    public ZoneId effectiveDbZoneId() {
        if (dbZoneId != null && !dbZoneId.isBlank()) {
            return ZoneId.of(dbZoneId.trim());
        }
        return isPatientAppointmentSource() ? ZoneId.of("UTC") : ZoneId.of(zoneId);
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
