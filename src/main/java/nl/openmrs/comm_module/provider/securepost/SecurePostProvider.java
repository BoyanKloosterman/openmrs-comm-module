package nl.openmrs.comm_module.provider.securepost;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Component;

@Component
public class SecurePostProvider implements MessagingProvider {

    private final SecurePostClient securePostClient;

    public SecurePostProvider(SecurePostClient securePostClient) {
        this.securePostClient = securePostClient;
    }

    @Override
    public MessagingProviderType getType() {
        return MessagingProviderType.SECUREPOST;
    }

    @Override
    public ProviderSendResult sendMessage(NotificationQueueMessage message) {
        SecurePostRequest request = new SecurePostRequest(
                "SMS",
                message.getRecipient(),
                message.getBody(),
                message.getSubject()
        );

        try {
            SecurePostResponse response = securePostClient.send(request);

            if (response == null) {
                return ProviderSendResult.failed("SecurePost returned an empty response");
            }

            if (!response.isDelivered()) {
                return ProviderSendResult.failed(response.getErrorMessage());
            }

            return ProviderSendResult.success(response.getTrackingId());
        } catch (SecurePostApiException exception) {
            return ProviderSendResult.failed(exception.getMessage());
        }
    }
}