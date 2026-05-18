package nl.openmrs.comm_module.provider.asyncflow;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.stereotype.Component;

@Component
public class AsyncFlowProvider implements MessagingProvider {

    private final AsyncFlowClient asyncFlowClient;

    public AsyncFlowProvider(AsyncFlowClient asyncFlowClient) {
        this.asyncFlowClient = asyncFlowClient;
    }

    @Override
    public MessagingProviderType getType() {
        return MessagingProviderType.ASYNCFLOW;
    }

    @Override
    public ProviderSendResult sendMessage(NotificationQueueMessage message) {
        AsyncFlowSubmitRequest request = new AsyncFlowSubmitRequest(
                message.getRecipient(),
                message.getBody(),
                "normal"
        );

        try {
            AsyncFlowSubmitResponse response = asyncFlowClient.submit(request);

            if (response == null) {
                return ProviderSendResult.failed("AsyncFlow returned an empty response");
            }

            if (!response.isAccepted()) {
                return ProviderSendResult.failed(response.getMessage());
            }

            return ProviderSendResult.submitted(response.getTrackingId());
        } catch (AsyncFlowApiException exception) {
            return ProviderSendResult.failed(exception.getMessage());
        }
    }
}