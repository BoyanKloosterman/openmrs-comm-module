package nl.openmrs.comm_module.provider;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;

public interface MessagingProvider {

    MessagingProviderType getType();

    ProviderSendResult sendMessage(NotificationQueueMessage message);
}