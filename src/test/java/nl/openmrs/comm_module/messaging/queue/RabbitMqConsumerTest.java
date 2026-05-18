package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMqConsumerTest {

    @Mock
    private MessagingProviderFactory providerFactory;

    @Mock
    private MessagingProvider messagingProvider;

    @Mock
    private NotificationDeliveryLogService deliveryLogService;

    @InjectMocks
    private RabbitMqConsumer consumer;

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
    }
}
