package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.RabbitMqConfig;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConsumer {

    private final MessagingProviderFactory providerFactory;
    private final NotificationDeliveryLogService deliveryLogService;

    public RabbitMqConsumer(
            MessagingProviderFactory providerFactory, NotificationDeliveryLogService deliveryLogService) {
        this.providerFactory = providerFactory;
        this.deliveryLogService = deliveryLogService;
    }

    @RabbitListener(queues = "#{'${messaging.queues}'.split(',')}")
    public void consume(NotificationQueueMessage message) {
        MessagingProvider provider = providerFactory.getProvider(message.getProvider());
        ProviderSendResult result = provider.sendMessage(message);
        deliveryLogService.recordProviderAttempt(message, result);
    }
}
