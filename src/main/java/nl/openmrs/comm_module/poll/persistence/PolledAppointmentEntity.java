package nl.openmrs.comm_module.poll.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

import nl.openmrs.comm_module.persistence.EncryptedStringConverter;

@Entity
@Table(
        name = "polled_appointment",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_polled_appointment_org_appointment",
                        columnNames = {"organisation_id", "appointment_fhir_id"}))
public class PolledAppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false, length = 128)
    private String organisationId;

    @Column(name = "appointment_uuid", nullable = false, length = 128)
    private String appointmentUuid;

    @Column(name = "appointment_fhir_id", nullable = false, length = 128)
    private String appointmentFhirId;

    @Column(name = "patient_fhir_id", nullable = false, length = 128)
    private String patientFhirId;

    @Column(name = "appointment_datetime", nullable = false)
    private Instant appointmentDatetime;

    /** Leesbare locatie (polikliniek/kamer); geen FHIR Location-uuid. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "location_id", length = 512)
    private String locationId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "appointment_type", length = 512)
    private String appointmentType;

    /** OpenMRS reason / FHIR instructie-extension. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "appointment_reason", length = 1024)
    private String appointmentReason;

    @Column(name = "voided", nullable = false)
    private boolean voided;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "patient_display_name", length = 512)
    private String patientDisplayName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "patient_phone", length = 128)
    private String patientPhone;

    @Column(name = "last_polled_at", nullable = false)
    private Instant lastPolledAt;

    /** Test-GUI: gekozen messaging provider voor deze afspraak. */
    @Column(name = "test_messaging_provider", length = 32)
    private String testMessagingProvider;

    public Long getId() {
        return id;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getAppointmentUuid() {
        return appointmentUuid;
    }

    public void setAppointmentUuid(String appointmentUuid) {
        this.appointmentUuid = appointmentUuid;
    }

    public String getAppointmentFhirId() {
        return appointmentFhirId;
    }

    public void setAppointmentFhirId(String appointmentFhirId) {
        this.appointmentFhirId = appointmentFhirId;
    }

    public String getPatientFhirId() {
        return patientFhirId;
    }

    public void setPatientFhirId(String patientFhirId) {
        this.patientFhirId = patientFhirId;
    }

    public Instant getAppointmentDatetime() {
        return appointmentDatetime;
    }

    public void setAppointmentDatetime(Instant appointmentDatetime) {
        this.appointmentDatetime = appointmentDatetime;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getAppointmentReason() {
        return appointmentReason;
    }

    public void setAppointmentReason(String appointmentReason) {
        this.appointmentReason = appointmentReason;
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

    public String getTestMessagingProvider() {
        return testMessagingProvider;
    }

    public void setTestMessagingProvider(String testMessagingProvider) {
        this.testMessagingProvider = testMessagingProvider;
    }
}
