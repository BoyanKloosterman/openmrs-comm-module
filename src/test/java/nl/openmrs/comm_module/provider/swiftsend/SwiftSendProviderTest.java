package nl.openmrs.comm_module.provider.swiftsend;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SwiftSendProviderTest {

    private SwiftSendClient swiftSendClient;
    private SwiftSendProvider swiftSendProvider;

    @BeforeEach
    void setUp() {
        swiftSendClient = mock(SwiftSendClient.class);
        swiftSendProvider = new SwiftSendProvider(swiftSendClient);
    }

    @Test
    void getType_returnsSwiftSend() {
        assertEquals(MessagingProviderType.SWIFTSEND, swiftSendProvider.getType());
    }

    @Test
    void sendMessage_whenSwiftSendReturnsSuccess_returnsSuccessfulResult() {
        SwiftSendResponse response = mock(SwiftSendResponse.class);

        when(response.isSuccess()).thenReturn(true);
        when(response.getMessageId()).thenReturn("message-123");
        when(response.getFailedRecipients()).thenReturn(List.of());

        when(swiftSendClient.send(any(SwiftSendRequest.class))).thenReturn(response);

        ProviderSendResult result = swiftSendProvider.sendMessage(createMessage());

        assertTrue(result.isSuccessful());
        assertEquals("SENT", result.getStatus());
        assertEquals("message-123", result.getProviderMessageId());
        assertNull(result.getErrorMessage());

        verify(swiftSendClient).send(any(SwiftSendRequest.class));
    }

    @Test
    void sendMessage_whenSwiftSendReturnsNull_returnsFailedResult() {
        when(swiftSendClient.send(any(SwiftSendRequest.class))).thenReturn(null);

        ProviderSendResult result = swiftSendProvider.sendMessage(createMessage());

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertNull(result.getProviderMessageId());
        assertEquals("SwiftSend returned an empty response", result.getErrorMessage());
    }

    @Test
    void sendMessage_whenSwiftSendReturnsUnsuccessfulResponse_returnsFailedResult() {
        SwiftSendResponse response = mock(SwiftSendResponse.class);

        when(response.isSuccess()).thenReturn(false);
        when(response.getError()).thenReturn("Invalid recipient");

        when(swiftSendClient.send(any(SwiftSendRequest.class))).thenReturn(response);

        ProviderSendResult result = swiftSendProvider.sendMessage(createMessage());

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertNull(result.getProviderMessageId());
        assertEquals("Invalid recipient", result.getErrorMessage());
    }

    @Test
    void sendMessage_whenSwiftSendHasFailedRecipients_returnsFailedResult() {
        SwiftSendResponse response = mock(SwiftSendResponse.class);

        when(response.isSuccess()).thenReturn(true);
        when(response.getMessageId()).thenReturn("message-123");
        when(response.getFailedRecipients()).thenReturn(List.of("+31698765432"));

        when(swiftSendClient.send(any(SwiftSendRequest.class))).thenReturn(response);

        ProviderSendResult result = swiftSendProvider.sendMessage(createMessage());

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertNull(result.getProviderMessageId());
        assertTrue(result.getErrorMessage().contains("+31698765432"));
    }

    @Test
    void sendMessage_whenClientThrowsApiException_returnsFailedResult() {
        when(swiftSendClient.send(any(SwiftSendRequest.class)))
                .thenThrow(new SwiftSendApiException("SwiftSend API error. Status: 401"));

        ProviderSendResult result = swiftSendProvider.sendMessage(createMessage());

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertNull(result.getProviderMessageId());
        assertEquals("SwiftSend API error. Status: 401", result.getErrorMessage());
    }

    private NotificationQueueMessage createMessage() {
        return new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31612345678",
                "Afspraak herinnering",
                "U heeft morgen om 10:00 een afspraak.",
                MessagingProviderType.SWIFTSEND,
                "APPOINTMENT_REMINDER",
                Instant.now()
        );
    }
}