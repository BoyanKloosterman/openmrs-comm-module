package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.RabbitMqConfig;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConsumer {

    private final MessagingProviderFactory providerFactory;

    public RabbitMqConsumer(MessagingProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void consume(NotificationQueueMessage message) {
        MessagingProvider provider = providerFactory.getProvider(message.getProvider());

        ProviderSendResult result = provider.sendMessage(message);

        System.out.println("Notification processed");
        System.out.println("Notification ID: " + message.getNotificationId());
        System.out.println("Provider: " + message.getProvider());
        System.out.println("Status: " + result.getStatus());
        System.out.println("Provider message ID: " + result.getProviderMessageId());
    }
}