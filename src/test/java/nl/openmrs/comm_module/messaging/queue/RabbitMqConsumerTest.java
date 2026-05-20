package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.message_log.MessageLogService;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMqConsumerTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private MessagingProviderFactory providerFactory;

    @Mock
    private MessagingProvider messagingProvider;

    @Mock
    private NotificationDeliveryLogService deliveryLogService;

    @Mock
    private MessageLogService messageLogService;

    @Mock
    private RabbitMqProducer rabbitMqProducer;

    private RabbitMqConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RabbitMqConsumer(
                providerFactory,
                deliveryLogService,
                messageLogService,
                rabbitMqProducer,
                MAX_ATTEMPTS);
    }

    @Test
    void logtVerzendstatusNaProviderPoging() {
        NotificationQueueMessage message = new NotificationQueueMessage();
        message.setNotificationId(UUID.randomUUID());
        message.setProvider(MessagingProviderType.SWIFTSEND);
        ProviderSendResult result = ProviderSendResult.success("ext-1");

        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(messagingProvider);
        when(messagingProvider.sendMessage(message)).thenReturn(result);

        consumer.consume(message);

        verify(messagingProvider).sendMessage(message);
        verify(deliveryLogService).recordProviderAttempt(message, result);
        verify(messageLogService).recordProviderAttempt(message, result);
        verify(rabbitMqProducer, never()).publishRetry(message);
    }

    @Test
    void plantRetryBijMisluktePoging() {
        NotificationQueueMessage message = new NotificationQueueMessage();
        message.setNotificationId(UUID.randomUUID());
        message.setProvider(MessagingProviderType.SWIFTSEND);
        ProviderSendResult result = ProviderSendResult.failed("timeout");

        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(messagingProvider);
        when(messagingProvider.sendMessage(message)).thenReturn(result);

        consumer.consume(message);

        verify(deliveryLogService).recordProviderAttempt(message, result);
        verify(messageLogService).recordProviderAttempt(message, result);
        verify(rabbitMqProducer).publishRetry(message);
    }

    @Test
    void weigertNaMaxRetries() {
        NotificationQueueMessage message = new NotificationQueueMessage();
        message.setNotificationId(UUID.randomUUID());
        message.setProvider(MessagingProviderType.SWIFTSEND);
        message.setRetryCount(MAX_ATTEMPTS);
        ProviderSendResult result = ProviderSendResult.failed("timeout");

        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(messagingProvider);
        when(messagingProvider.sendMessage(message)).thenReturn(result);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consume(message));
        verify(messageLogService).recordProviderAttempt(message, result);
        verify(rabbitMqProducer, never()).publishRetry(message);
    }
}
