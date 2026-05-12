package nl.openmrs.comm_module.provider.swiftsend;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SwiftSendProvider implements MessagingProvider {

    private final SwiftSendClient swiftSendClient;

    public SwiftSendProvider(SwiftSendClient swiftSendClient) {
        this.swiftSendClient = swiftSendClient;
    }

    @Override
    public MessagingProviderType getType() {
        return MessagingProviderType.SWIFTSEND;
    }

    @Override
    public ProviderSendResult sendMessage(NotificationQueueMessage message) {
        SwiftSendRequest request = new SwiftSendRequest(
                "SMS",
                List.of(message.getRecipient()),
                message.getBody()
        );

        try {
            SwiftSendResponse response = swiftSendClient.send(request);

            if (response == null) {
                return ProviderSendResult.failed("SwiftSend returned an empty response");
            }

            if (!response.isSuccess()) {
                return ProviderSendResult.failed(response.getError());
            }

            if (response.getFailedRecipients() != null && !response.getFailedRecipients().isEmpty()) {
                return ProviderSendResult.failed(
                        "SwiftSend failed for recipients: " + response.getFailedRecipients()
                );
            }

            return ProviderSendResult.success(response.getMessageId());
        } catch (SwiftSendApiException exception) {
            return ProviderSendResult.failed(exception.getMessage());
        }
    }
}