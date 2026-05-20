package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.message_log.MessageLogService;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConsumer.class);

    private final MessagingProviderFactory providerFactory;
    private final NotificationDeliveryLogService deliveryLogService;
    private final MessageLogService messageLogService;
    private final RabbitMqProducer rabbitMqProducer;
    private final int maxAttempts;

    public RabbitMqConsumer(
            MessagingProviderFactory providerFactory,
            NotificationDeliveryLogService deliveryLogService,
            MessageLogService messageLogService,
            RabbitMqProducer rabbitMqProducer,
            @Value("${messaging.retry.max-attempts}") int maxAttempts) {
        this.providerFactory = providerFactory;
        this.deliveryLogService = deliveryLogService;
        this.messageLogService = messageLogService;
        this.rabbitMqProducer = rabbitMqProducer;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = "#{'${messaging.queues}'.split(',')}")
    public void consume(NotificationQueueMessage message) {
        MessagingProvider provider = providerFactory.getProvider(message.getProvider());
        ProviderSendResult result = provider.sendMessage(message);
        deliveryLogService.recordProviderAttempt(message, result);
        messageLogService.recordProviderAttempt(message, result);

        if (!result.isSuccessful()) {
            handleFailedMessage(message, result);
        }
    }

    private void handleFailedMessage(NotificationQueueMessage message, ProviderSendResult result) {
        if (message.getRetryCount() < maxAttempts) {
            message.incrementRetryCount();
            log.warn(
                    "Retry notificatie notificationId={} poging={} van {}: {}",
                    message.getNotificationId(),
                    message.getRetryCount(),
                    maxAttempts,
                    result.getErrorMessage());
            rabbitMqProducer.publishRetry(message);
            return;
        }

        log.error(
                "Max retry bereikt voor notificationId={}: {}",
                message.getNotificationId(),
                result.getErrorMessage());
        throw new AmqpRejectAndDontRequeueException(
                "Notification failed after "
                        + maxAttempts
                        + " retry attempts. Last error: "
                        + result.getErrorMessage());
    }
}
