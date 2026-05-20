package nl.openmrs.comm_module.messaging.queue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitMqConsumerTest {

    private MessagingProviderFactory providerFactory;
    private RabbitMqProducer rabbitMqProducer;
    private MessagingProvider provider;
    private SimpleMeterRegistry meterRegistry;
    private RabbitMqConsumer consumer;

    @BeforeEach
    void setUp() {
        providerFactory = mock(MessagingProviderFactory.class);
        rabbitMqProducer = mock(RabbitMqProducer.class);
        provider = mock(MessagingProvider.class);
        meterRegistry = new SimpleMeterRegistry();

        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(provider);

        consumer = new RabbitMqConsumer(
                providerFactory,
                rabbitMqProducer,
                meterRegistry,
                3,
                1000,
                2.0
        );
    }

    @Test
    void consume_whenProviderSucceeds_doesNotRetry() {
        NotificationQueueMessage message = createMessage();
        when(provider.sendMessage(any(NotificationQueueMessage.class)))
                .thenReturn(ProviderSendResult.success("message-1"));

        consumer.consume(message);

        verify(rabbitMqProducer, never()).publishRetry(any(NotificationQueueMessage.class), anyLong());
        assertEquals(1.0, meterRegistry.counter(
                "comm.messaging.notifications",
                "provider", "SWIFTSEND",
                "status", "SENT"
        ).count());
    }

    @Test
    void consume_whenProviderFails_schedulesRetryWithBackoff() {
        NotificationQueueMessage message = createMessage();
        when(provider.sendMessage(any(NotificationQueueMessage.class)))
                .thenReturn(ProviderSendResult.failed("timeout"));

        consumer.consume(message);

        verify(rabbitMqProducer).publishRetry(message, 1000L);
        assertEquals(1, message.getRetryCount());
        assertEquals(1.0, meterRegistry.counter(
                "comm.messaging.retry",
                "provider", "SWIFTSEND",
                "action", "scheduled"
        ).count());
    }

    @Test
    void consume_whenMaxAttemptsReached_routesToDlq() {
        NotificationQueueMessage message = createMessage();
        message.setRetryCount(3);
        when(provider.sendMessage(any(NotificationQueueMessage.class)))
                .thenReturn(ProviderSendResult.failed("timeout"));

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consume(message));

        verify(rabbitMqProducer, never()).publishRetry(any(NotificationQueueMessage.class), anyLong());
        assertTrue(meterRegistry.counter(
                "comm.messaging.retry",
                "provider", "SWIFTSEND",
                "action", "exhausted"
        ).count() > 0.0);
    }

    private NotificationQueueMessage createMessage() {
        NotificationQueueMessage message = new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31612345678",
                "Test subject",
                "Test message",
                MessagingProviderType.SWIFTSEND,
                "SMS",
                Instant.now()
        );
        message.setRetryCount(0);
        return message;
    }
}
