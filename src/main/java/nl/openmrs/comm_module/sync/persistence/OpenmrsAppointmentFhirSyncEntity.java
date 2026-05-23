package nl.openmrs.comm_module.sync.persistence;

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
        name = "openmrs_appointment_fhir_sync",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_omrs_appt_fhir_sync_appt_id",
                        columnNames = {"openmrs_appointment_id"}))
public class OpenmrsAppointmentFhirSyncEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "openmrs_appointment_id", nullable = false)
    private int openmrsAppointmentId;

    @Column(name = "fhir_appointment_id", nullable = false, length = 128)
    private String fhirAppointmentId;

    @Column(name = "fhir_patient_id", nullable = false, length = 128)
    private String fhirPatientId;

    @Column(name = "last_sync_token", nullable = false, length = 512)
    private String lastSyncToken;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    public Long getId() {
        return id;
    }

    public int getOpenmrsAppointmentId() {
        return openmrsAppointmentId;
    }

    public void setOpenmrsAppointmentId(int openmrsAppointmentId) {
        this.openmrsAppointmentId = openmrsAppointmentId;
    }

    public String getFhirAppointmentId() {
        return fhirAppointmentId;
    }

    public void setFhirAppointmentId(String fhirAppointmentId) {
        this.fhirAppointmentId = fhirAppointmentId;
    }

    public String getFhirPatientId() {
        return fhirPatientId;
    }

    public void setFhirPatientId(String fhirPatientId) {
        this.fhirPatientId = fhirPatientId;
    }

    public String getLastSyncToken() {
        return lastSyncToken;
    }

    public void setLastSyncToken(String lastSyncToken) {
        this.lastSyncToken = lastSyncToken;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
