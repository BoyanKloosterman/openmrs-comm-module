package nl.openmrs.comm_module.poll.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "polled_encounter",
        uniqueConstraints = @UniqueConstraint(name = "uk_polled_encounter_org_encounter", columnNames = {"organisation_id", "encounter_fhir_id"}))
public class PolledEncounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false, length = 128)
    private String organisationId;

    /** Stabiele UUID uit mapper (omschrijving OpenMRS). */
    @Column(name = "encounter_uuid", nullable = false, length = 128)
    private String encounterUuid;

    @Column(name = "encounter_fhir_id", nullable = false, length = 128)
    private String encounterFhirId;

    @Column(name = "patient_fhir_id", nullable = false, length = 128)
    private String patientFhirId;

    @Column(name = "encounter_datetime", nullable = false)
    private Instant encounterDatetime;

    @Column(name = "location_id", length = 128)
    private String locationId;

    @Column(name = "encounter_type", length = 512)
    private String encounterType;

    @Column(name = "voided", nullable = false)
    private boolean voided;

    @Column(name = "patient_display_name", length = 512)
    private String patientDisplayName;

    @Column(name = "patient_phone", length = 128)
    private String patientPhone;

    @Column(name = "last_polled_at", nullable = false)
    private Instant lastPolledAt;

    public Long getId() {
        return id;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getEncounterUuid() {
        return encounterUuid;
    }

    public void setEncounterUuid(String encounterUuid) {
        this.encounterUuid = encounterUuid;
    }

    public String getEncounterFhirId() {
        return encounterFhirId;
    }

    public void setEncounterFhirId(String encounterFhirId) {
        this.encounterFhirId = encounterFhirId;
    }

    public String getPatientFhirId() {
        return patientFhirId;
    }

    public void setPatientFhirId(String patientFhirId) {
        this.patientFhirId = patientFhirId;
    }

    public Instant getEncounterDatetime() {
        return encounterDatetime;
    }

    public void setEncounterDatetime(Instant encounterDatetime) {
        this.encounterDatetime = encounterDatetime;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(String encounterType) {
        this.encounterType = encounterType;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public String getPatientDisplayName() {
        return patientDisplayName;
    }

    public void setPatientDisplayName(String patientDisplayName) {
        this.patientDisplayName = patientDisplayName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public Instant getLastPolledAt() {
        return lastPolledAt;
    }

    public void setLastPolledAt(Instant lastPolledAt) {
        this.lastPolledAt = lastPolledAt;
    }
}
