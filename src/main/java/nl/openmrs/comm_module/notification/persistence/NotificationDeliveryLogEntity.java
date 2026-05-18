package nl.openmrs.comm_module.notification.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** US-001-5: verzendpoging zonder persoonsgegevens (US-014 breidt metadata later uit). */
@Entity
@Table(name = "notification_delivery_log")
public class NotificationDeliveryLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "encounter_fhir_id", length = 128)
    private String encounterFhirId;

    @Column(name = "message_type", nullable = false, length = 64)
    private String messageType;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "successful", nullable = false)
    private boolean successful;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    public Long getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public String getEncounterFhirId() {
        return encounterFhirId;
    }

    public void setEncounterFhirId(String encounterFhirId) {
        this.encounterFhirId = encounterFhirId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}
