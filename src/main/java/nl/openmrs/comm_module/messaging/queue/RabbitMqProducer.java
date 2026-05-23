package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.RabbitMqConfig;
import nl.openmrs.comm_module.metrics.MessagingMetrics;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqProducer {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingMetrics metrics;
    private final long initialDelayMs;
    private final double multiplier;
    private final long maxDelayMs;

    public RabbitMqProducer(
            RabbitTemplate rabbitTemplate,
            MessagingMetrics metrics,
            @Value("${messaging.retry.initial-delay-ms}") long initialDelayMs,
            @Value("${messaging.retry.multiplier}") double multiplier,
            @Value("${messaging.retry.max-delay-ms}") long maxDelayMs
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.metrics = metrics;
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
        this.maxDelayMs = maxDelayMs;
    }

    public void publish(NotificationQueueMessage message) {
        sendToProviderQueue(message);
        metrics.recordQueued(message, false);
    }

    public void publishRetry(NotificationQueueMessage message) {
        long delayMs = calculateRetryDelay(message.getRetryCount());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RETRY_EXCHANGE,
                message.getProvider().getRoutingKey(),
                message,
                rabbitMessage -> {
                    rabbitMessage.getMessageProperties().setExpiration(String.valueOf(delayMs));
                    return rabbitMessage;
                }
        );

        metrics.recordQueued(message, true);

        System.out.println("Retry message published with delay: " + delayMs + " ms");
    }

    private void sendToProviderQueue(NotificationQueueMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                message.getProvider().getRoutingKey(),
                message
        );
    }

    private long calculateRetryDelay(int retryCount) {
        int exponent = Math.max(retryCount - 1, 0);
        double delay = initialDelayMs * Math.pow(multiplier, exponent);
        return Math.min((long) delay, maxDelayMs);
    }
}