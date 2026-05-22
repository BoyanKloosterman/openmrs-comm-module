package nl.openmrs.comm_module.provider.swiftsend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SwiftSendProvider implements MessagingProvider {

    private final SwiftSendClient swiftSendClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SwiftSendProvider(SwiftSendClient swiftSendClient) {
        this.swiftSendClient = swiftSendClient;
    }

    @Override
    public MessagingProviderType getType() {
        return MessagingProviderType.SWIFTSEND;
    }

    @Override
    public ProviderSendResult sendMessage(NotificationQueueMessage message, String credentialsJson) {
        try {
            JsonNode credentials = objectMapper.readTree(credentialsJson);
            String apiKey = credentials.path("apiKey").asText(null);

            if (apiKey == null || apiKey.isBlank()) {
                return ProviderSendResult.failed("SwiftSend credentials missing apiKey");
            }

            SwiftSendRequest request = new SwiftSendRequest(
                    "SMS",
                    List.of(message.getRecipient()),
                    message.getBody()
            );

            SwiftSendResponse response = swiftSendClient.send(request, apiKey);

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
        } catch (Exception exception) {
            return ProviderSendResult.failed("SwiftSend credential parsing failed: " + exception.getMessage());
        }
    }
}