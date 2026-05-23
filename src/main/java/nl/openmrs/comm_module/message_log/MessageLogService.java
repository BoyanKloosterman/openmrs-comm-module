package nl.openmrs.comm_module.message_log;

import nl.openmrs.comm_module.message_log.persistence.MessageLogEntity;
import nl.openmrs.comm_module.message_log.persistence.MessageLogRepository;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** US-014: slaat provider-metadata op zonder PII. */
@Service
public class MessageLogService {

    private final MessageLogRepository repository;
    private final Clock clock;

    public MessageLogService(MessageLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void recordProviderAttempt(NotificationQueueMessage message, ProviderSendResult result) {
        Instant sentAt = clock.instant();
        MessageLogEntity entry = new MessageLogEntity();
        entry.setNotificationId(message.getNotificationId());
        entry.setProvider(message.getProvider() != null ? message.getProvider().name() : "UNKNOWN");
        entry.setMessageType(message.getMessageType());
        entry.setStatus(result.getStatus());
        entry.setProviderMessageId(result.getProviderMessageId());
        entry.setSuccessful(result.isSuccessful());
        entry.setQueuedAt(message.getQueuedAt());
        entry.setSentAt(sentAt);
        entry.setRetryCount(message.getRetryCount());
        repository.save(entry);
    }
}
