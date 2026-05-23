package nl.openmrs.comm_module.provider.asyncflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Component;

@Component
public class AsyncFlowProvider implements MessagingProvider {

    private final AsyncFlowClient asyncFlowClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AsyncFlowProvider(AsyncFlowClient asyncFlowClient) {
        this.asyncFlowClient = asyncFlowClient;
    }

    @Override
    public MessagingProviderType getType() {
        return MessagingProviderType.ASYNCFLOW;
    }

    @Override
    public ProviderSendResult sendMessage(NotificationQueueMessage message, String credentialsJson) {
        try {
            JsonNode credentials = objectMapper.readTree(credentialsJson);
            String apiKey = credentials.path("apiKey").asText(null);

            if (apiKey == null || apiKey.isBlank()) {
                return ProviderSendResult.failed("AsyncFlow credentials missing apiKey");
            }

            AsyncFlowSubmitRequest request = new AsyncFlowSubmitRequest(
                    message.getRecipient(),
                    message.getBody(),
                    "normal"
            );

            AsyncFlowSubmitResponse response = asyncFlowClient.submit(request, apiKey);

            if (response == null) {
                return ProviderSendResult.failed("AsyncFlow returned an empty response");
            }

            if (!response.isAccepted()) {
                return ProviderSendResult.failed(response.getMessage());
            }

            return ProviderSendResult.submitted(response.getTrackingId());
        } catch (AsyncFlowApiException exception) {
            return ProviderSendResult.failed(exception.getMessage());
        } catch (Exception exception) {
            return ProviderSendResult.failed(
                    "AsyncFlow credential parsing failed: " + exception.getMessage()
            );
        }
    }
}