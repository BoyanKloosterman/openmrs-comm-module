package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogEntity;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** US-001-5: verzendstatus vastleggen na queue en na provider-poging. */
@Service
public class NotificationDeliveryLogService {

    public static final String STATUS_QUEUED = "QUEUED";

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryLogService.class);

    private final NotificationDeliveryLogRepository repository;
    private final Clock clock;

    public NotificationDeliveryLogService(NotificationDeliveryLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void recordQueued(NotificationQueueMessage message) {
        persist(message, STATUS_QUEUED, null, null, true);
    }

    @Transactional
    public void recordProviderAttempt(NotificationQueueMessage message, ProviderSendResult result) {
        persist(
                message,
                result.getStatus(),
                result.getProviderMessageId(),
                result.getErrorMessage(),
                result.isSuccessful());
    }

    public boolean hasSuccessfulDelivery(String appointmentFhirId, String messageType) {
        if (appointmentFhirId == null || appointmentFhirId.isBlank() || messageType == null || messageType.isBlank()) {
            return false;
        }
        return repository.existsByAppointmentFhirIdAndMessageTypeAndSuccessfulTrue(appointmentFhirId, messageType);
    }

    private void persist(
            NotificationQueueMessage message,
            String status,
            String providerMessageId,
            String errorMessage,
            boolean successful) {
        Instant attemptedAt = clock.instant();
        NotificationDeliveryLogEntity entry = new NotificationDeliveryLogEntity();
        entry.setNotificationId(message.getNotificationId());
        entry.setAppointmentFhirId(message.getAppointmentFhirId());
        entry.setMessageType(message.getMessageType());
        entry.setProvider(message.getProvider() != null ? message.getProvider().name() : "UNKNOWN");
        entry.setStatus(status);
        entry.setProviderMessageId(providerMessageId);
        entry.setErrorMessage(truncateError(errorMessage));
        entry.setSuccessful(successful);
        entry.setAttemptedAt(attemptedAt);
        repository.save(entry);

        if (successful) {
            log.info(
                    "Verzendstatus {}: notificationId={} appointment={} provider={} providerMsgId={}",
                    status,
                    message.getNotificationId(),
                    message.getAppointmentFhirId(),
                    entry.getProvider(),
                    providerMessageId);
        } else {
            log.warn(
                    "Verzendstatus {}: notificationId={} appointment={} provider={} fout={}",
                    status,
                    message.getNotificationId(),
                    message.getAppointmentFhirId(),
                    entry.getProvider(),
                    entry.getErrorMessage());
        }
    }

    private static String truncateError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() > 1024 ? errorMessage.substring(0, 1024) : errorMessage;
    }
}
