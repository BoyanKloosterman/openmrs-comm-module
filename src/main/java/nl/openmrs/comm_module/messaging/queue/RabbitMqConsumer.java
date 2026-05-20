package nl.openmrs.comm_module.messaging.queue;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import nl.openmrs.comm_module.metrics.MessagingMetrics;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConsumer {

    private final MessagingProviderFactory providerFactory;
    private final RabbitMqProducer rabbitMqProducer;
    private final MessagingMetrics metrics;
    private final int maxAttempts;

    public RabbitMqConsumer(
            MessagingProviderFactory providerFactory,
            RabbitMqProducer rabbitMqProducer,
            MessagingMetrics metrics,
            @Value("${messaging.retry.max-attempts}") int maxAttempts
    ) {
        this.providerFactory = providerFactory;
        this.rabbitMqProducer = rabbitMqProducer;
        this.metrics = metrics;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = "#{'${messaging.queues}'.split(',')}")
    public void consume(NotificationQueueMessage message) {
        metrics.recordDequeued(message);
        MessagingProvider provider = providerFactory.getProvider(message.getProvider());

        ProviderSendResult result = provider.sendMessage(message);

        metrics.recordSendResult(message, result);

        logResult(message, result);

        if (!result.isSuccessful()) {
            handleFailedMessage(message, result);
        }
    }

    private void handleFailedMessage(NotificationQueueMessage message, ProviderSendResult result) {
        System.out.println("Error message: " + result.getErrorMessage());

        if (message.getRetryCount() < maxAttempts) {
            message.incrementRetryCount();

            System.out.println("Retrying notification");
            System.out.println("Notification ID: " + message.getNotificationId());
            System.out.println("Retry attempt: " + message.getRetryCount() + " of " + maxAttempts);

            rabbitMqProducer.publishRetry(message);
            return;
        }

        System.out.println("Max retry attempts reached");
        System.out.println("Notification will be rejected and routed to DLQ");
        System.out.println("Notification ID: " + message.getNotificationId());

        throw new AmqpRejectAndDontRequeueException(
                "Notification failed after " + maxAttempts + " retry attempts. Last error: " + result.getErrorMessage()
        );
    }

    private void logResult(NotificationQueueMessage message, ProviderSendResult result) {
        System.out.println("Notification processed");
        System.out.println("Notification ID: " + message.getNotificationId());
        System.out.println("Provider: " + message.getProvider());
        System.out.println("Status: " + result.getStatus());
        System.out.println("Provider message ID: " + result.getProviderMessageId());
        System.out.println("Retry count: " + message.getRetryCount());
    }
}