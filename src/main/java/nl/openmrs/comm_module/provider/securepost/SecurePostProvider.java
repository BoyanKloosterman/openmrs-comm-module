package nl.openmrs.comm_module.provider.securepost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Component;

@Component
public class SecurePostProvider implements MessagingProvider {

    private final SecurePostClient securePostClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurePostProvider(SecurePostClient securePostClient) {
        this.securePostClient = securePostClient;
    }

    @Override
    public MessagingProviderType getType() {
        return MessagingProviderType.SECUREPOST;
    }

    @Override
    public ProviderSendResult sendMessage(NotificationQueueMessage message, String credentialsJson) {
        try {
            JsonNode credentials = objectMapper.readTree(credentialsJson);
            String clientId = credentials.path("clientId").asText(null);
            String clientSecret = credentials.path("clientSecret").asText(null);

            if (clientId == null || clientId.isBlank()
                    || clientSecret == null || clientSecret.isBlank()) {
                return ProviderSendResult.failed("SecurePost credentials missing clientId or clientSecret");
            }

            SecurePostRequest request = new SecurePostRequest(
                    "SMS",
                    message.getRecipient(),
                    message.getBody(),
                    message.getSubject()
            );

            SecurePostResponse response = securePostClient.send(
                    request,
                    clientId,
                    clientSecret
            );

            if (response == null) {
                return ProviderSendResult.failed("SecurePost returned an empty response");
            }

            if (!response.isDelivered()) {
                return ProviderSendResult.failed(response.getErrorMessage());
            }

            return ProviderSendResult.success(response.getTrackingId());
        } catch (SecurePostApiException exception) {
            return ProviderSendResult.failed(exception.getMessage());
        } catch (Exception exception) {
            return ProviderSendResult.failed(
                    "SecurePost credential parsing failed: " + exception.getMessage()
            );
        }
    }
}