package nl.openmrs.comm_module.poll.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/** FHIR-poll slaat deze afspraak over (verleden of handmatig uitgesloten). */
@Entity
@Table(
        name = "polled_appointment_exclusion",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_poll_exclusion_org_appointment",
                        columnNames = {"organisation_id", "appointment_fhir_id"}))
public class PolledAppointmentExclusionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false, length = 128)
    private String organisationId;

    @Column(name = "appointment_fhir_id", nullable = false, length = 128)
    private String appointmentFhirId;

    @Column(name = "excluded_at", nullable = false)
    private Instant excludedAt;

    public Long getId() {
        return id;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getAppointmentFhirId() {
        return appointmentFhirId;
    }

    public void setAppointmentFhirId(String appointmentFhirId) {
        this.appointmentFhirId = appointmentFhirId;
    }

    public Instant getExcludedAt() {
        return excludedAt;
    }

    public void setExcludedAt(Instant excludedAt) {
        this.excludedAt = excludedAt;
    }
}
