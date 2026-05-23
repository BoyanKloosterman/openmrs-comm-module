package nl.openmrs.comm_module.provider.legacylink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public ProviderSendResult sendMessage(NotificationQueueMessage message, String credentialsJson) {
        try {
            JsonNode credentials = objectMapper.readTree(credentialsJson);
            String username = credentials.path("username").asText(null);
            String password = credentials.path("password").asText(null);

            if (username == null || username.isBlank()
                    || password == null || password.isBlank()) {
                return ProviderSendResult.failed("LegacyLink credentials missing username or password");
            }

            LegacyLinkSoapRequest request = new LegacyLinkSoapRequest(
                    message.getRecipient(),
                    message.getBody(),
                    senderIdentification
            );

            LegacyLinkSoapResponse response = legacyLinkClient.sendSms(
                    request,
                    username,
                    password
            );

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
        } catch (Exception exception) {
            return ProviderSendResult.failed(
                    "LegacyLink credential parsing failed: " + exception.getMessage()
            );
        }
    }
}