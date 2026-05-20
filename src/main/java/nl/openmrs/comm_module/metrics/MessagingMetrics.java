package nl.openmrs.comm_module.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MessagingMetrics {

    private static final String METRIC_QUEUED_TOTAL = "comm_module_messages_queued_total";
    private static final String METRIC_DEQUEUED_TOTAL = "comm_module_messages_dequeued_total";
    private static final String METRIC_PROCESSED_TOTAL = "comm_module_messages_processed_total";
    private static final String METRIC_FAILED_TOTAL = "comm_module_messages_failed_total";
    private static final String METRIC_ERROR_TOTAL = "comm_module_message_errors_total";
    private static final String METRIC_IN_QUEUE = "comm_module_messages_in_queue";

    private final MeterRegistry registry;
    private final AtomicInteger inQueue;

    public MessagingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.inQueue = registry.gauge(METRIC_IN_QUEUE, new AtomicInteger(0));
    }

    public void recordQueued(NotificationQueueMessage message, boolean retry) {
        inQueue.incrementAndGet();
        counter(METRIC_QUEUED_TOTAL, baseTags(message).and("retry", String.valueOf(retry))).increment();
    }

    public void recordDequeued(NotificationQueueMessage message) {
        int updated = inQueue.decrementAndGet();
        if (updated < 0) {
            inQueue.set(0);
        }
        counter(METRIC_DEQUEUED_TOTAL, baseTags(message)).increment();
    }

    public void recordSendResult(NotificationQueueMessage message, ProviderSendResult result) {
        String status = safeTagValue(result.getStatus());
        counter(METRIC_PROCESSED_TOTAL, baseTags(message).and("status", status)).increment();

        if (!result.isSuccessful()) {
            counter(METRIC_FAILED_TOTAL, baseTags(message).and("status", status)).increment();
            if (result.getErrorMessage() != null && !result.getErrorMessage().isBlank()) {
                counter(METRIC_ERROR_TOTAL, baseTags(message).and("error_type", "provider")).increment();
            }
        }
    }

    private Counter counter(String name, Tags tags) {
        return registry.counter(name, tags);
    }

    private Tags baseTags(NotificationQueueMessage message) {
        String provider = message.getProvider() == null ? "unknown" : message.getProvider().name().toLowerCase();
        String messageType = safeTagValue(message.getMessageType());
        return Tags.of("provider", provider, "message_type", messageType);
    }

    private String safeTagValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }
}
