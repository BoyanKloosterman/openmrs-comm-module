package nl.openmrs.comm_module.messaging.queue;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RabbitMqConsumerTest {

    private MessagingProviderFactory providerFactory;
    private RabbitMqProducer rabbitMqProducer;
    private MessagingProvider messagingProvider;
    private RabbitMqConsumer rabbitMqConsumer;

    @BeforeEach
    void setUp() {
        providerFactory = mock(MessagingProviderFactory.class);
        rabbitMqProducer = mock(RabbitMqProducer.class);
        messagingProvider = mock(MessagingProvider.class);

        rabbitMqConsumer = new RabbitMqConsumer(
                providerFactory,
                rabbitMqProducer,
                3
        );
    }

    @Test
    void consumeDoesNotRetryWhenProviderSendIsSuccessful() {
        NotificationQueueMessage message = createMessage();
        ProviderSendResult result = ProviderSendResult.success("provider-message-id");

        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND))
                .thenReturn(messagingProvider);
        when(messagingProvider.sendMessage(message))
                .thenReturn(result);

        rabbitMqConsumer.consume(message);

        verify(rabbitMqProducer, never()).publishRetry(any());
        assertEquals(0, message.getRetryCount());
    }

    @Test
    void consumePublishesRetryWhenProviderSendFailsAndRetriesAreAvailable() {
        NotificationQueueMessage message = createMessage();
        message.setRetryCount(1);

        ProviderSendResult result = ProviderSendResult.failed("Invalid API key");

        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND))
                .thenReturn(messagingProvider);
        when(messagingProvider.sendMessage(message))
                .thenReturn(result);

        rabbitMqConsumer.consume(message);

        assertEquals(2, message.getRetryCount());
        verify(rabbitMqProducer).publishRetry(message);
    }

    @Test
    void consumeRejectsMessageWhenMaxRetriesAreReached() {
        NotificationQueueMessage message = createMessage();
        message.setRetryCount(3);

        ProviderSendResult result = ProviderSendResult.failed("Invalid API key");

        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND))
                .thenReturn(messagingProvider);
        when(messagingProvider.sendMessage(message))
                .thenReturn(result);

        assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> rabbitMqConsumer.consume(message)
        );

        verify(rabbitMqProducer, never()).publishRetry(any());
    }

    private NotificationQueueMessage createMessage() {
        return new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31612345678",
                "Test subject",
                "Test body",
                MessagingProviderType.SWIFTSEND,
                "SMS",
                Instant.now()
        );
    }
}