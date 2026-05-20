package nl.openmrs.comm_module.messaging.queue;

import io.micrometer.core.instrument.MeterRegistry;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqConsumer.class);
    private static final String METRIC_NOTIFICATIONS = "comm.messaging.notifications";
    private static final String METRIC_RETRY = "comm.messaging.retry";

    private final MessagingProviderFactory providerFactory;
    private final RabbitMqProducer rabbitMqProducer;
    private final MeterRegistry meterRegistry;
    private final int maxAttempts;
    private final int initialDelayMs;
    private final double retryMultiplier;

    public RabbitMqConsumer(
            MessagingProviderFactory providerFactory,
            RabbitMqProducer rabbitMqProducer,
            MeterRegistry meterRegistry,
            @Value("${messaging.retry.max-attempts}") int maxAttempts,
            @Value("${messaging.retry.initial-delay-ms}") int initialDelayMs,
            @Value("${messaging.retry.multiplier}") double retryMultiplier
    ) {
        this.providerFactory = providerFactory;
        this.rabbitMqProducer = rabbitMqProducer;
        this.meterRegistry = meterRegistry;
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.retryMultiplier = retryMultiplier;
    }

    @RabbitListener(queues = "#{'${messaging.queues}'.split(',')}")
    public void consume(NotificationQueueMessage message) {
        MessagingProvider provider = providerFactory.getProvider(message.getProvider());

        ProviderSendResult result = provider.sendMessage(message);

        logResult(message, result);
        meterRegistry.counter(
                METRIC_NOTIFICATIONS,
                "provider", message.getProvider().name(),
                "status", result.getStatus()
        ).increment();

        if (!result.isSuccessful()) {
            handleFailedMessage(message, result);
        }
    }

    private void handleFailedMessage(NotificationQueueMessage message, ProviderSendResult result) {
        LOGGER.warn("Provider send failed: {}", result.getErrorMessage());

        if (message.getRetryCount() < maxAttempts) {
            message.incrementRetryCount();
            long delayMs = calculateDelayMs(message.getRetryCount());

            LOGGER.info("Retrying notification: id={}, attempt={} of {}, delayMs={}",
                    message.getNotificationId(), message.getRetryCount(), maxAttempts, delayMs);

            meterRegistry.counter(
                    METRIC_RETRY,
                    "provider", message.getProvider().name(),
                    "action", "scheduled"
            ).increment();

            rabbitMqProducer.publishRetry(message, delayMs);
            return;
        }

        LOGGER.error("Max retry attempts reached. Notification archived to DLQ: id={}",
                message.getNotificationId());

        meterRegistry.counter(
                METRIC_RETRY,
                "provider", message.getProvider().name(),
                "action", "exhausted"
        ).increment();

        throw new AmqpRejectAndDontRequeueException(
                "Notification failed after " + maxAttempts + " retry attempts. Last error: " + result.getErrorMessage()
        );
    }

    private long calculateDelayMs(int retryCount) {
        double multiplierFactor = Math.pow(retryMultiplier, Math.max(0, retryCount - 1));
        double rawDelay = initialDelayMs * multiplierFactor;
        if (rawDelay > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0L, Math.round(rawDelay));
    }

    private void logResult(NotificationQueueMessage message, ProviderSendResult result) {
        LOGGER.info("Notification processed: id={}, provider={}, status={}, providerMessageId={}, retryCount={}",
                message.getNotificationId(),
                message.getProvider(),
                result.getStatus(),
                result.getProviderMessageId(),
                message.getRetryCount());
    }
}