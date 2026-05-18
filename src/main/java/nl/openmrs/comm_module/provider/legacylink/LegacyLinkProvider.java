package nl.openmrs.comm_module.provider.legacylink;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LegacyLinkProvider implements MessagingProvider {

    private final LegacyLinkClient legacyLinkClient;
    private final String senderIdentification;

    public LegacyLinkProvider(
            LegacyLinkClient legacyLinkClient,
            @Value("${providers.legacylink.sender}") String senderIdentification
    ) {
        this.legacyLinkClient = legacyLinkClient;
        this.senderIdentification = senderIdentification;
    }

    @Override
    public MessagingProviderType getType() {
        return MessagingProviderType.LEGACYLINK;
    }

    @Override
    public ProviderSendResult sendMessage(NotificationQueueMessage message) {
        LegacyLinkSoapRequest request = new LegacyLinkSoapRequest(
                message.getRecipient(),
                message.getBody(),
                senderIdentification
        );

        try {
            LegacyLinkSoapResponse response = legacyLinkClient.sendSms(request);

            if (!response.isSuccessful()) {
                return ProviderSendResult.failed(
                        "LegacyLink failed. StatusCode: "
                                + response.getStatusCode()
                                + ". Message: "
                                + response.getStatusMessage()
                );
            }

            return ProviderSendResult.success(response.getMessageReference());
        } catch (LegacyLinkApiException exception) {
            return ProviderSendResult.failed(exception.getMessage());
        }
    }
}