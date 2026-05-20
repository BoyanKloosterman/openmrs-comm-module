package nl.openmrs.comm_module.message_log.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** US-014: metadata voor factuurcontrole, zonder directe persoonsgegevens. */
@Entity
@Table(name = "message_log")
public class MessageLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "message_type", nullable = false, length = 64)
    private String messageType;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Column(name = "successful", nullable = false)
    private boolean successful;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    public Long getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
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

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
