package nl.openmrs.comm_module.provider.securepost;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecurePostProviderTest {

    private static final String CREDENTIALS_JSON =
            "{\"clientId\":\"securepost-client\",\"clientSecret\":\"securepost-secret-key\"}";

    private SecurePostClient securePostClient;
    private SecurePostProvider securePostProvider;

    @BeforeEach
    void setUp() {
        securePostClient = mock(SecurePostClient.class);
        securePostProvider = new SecurePostProvider(securePostClient);
    }

    @Test
    void getTypeReturnsSecurePost() {
        assertEquals(MessagingProviderType.SECUREPOST, securePostProvider.getType());
    }

    @Test
    void sendMessageReturnsSuccessWhenMessageIsDelivered() {
        SecurePostResponse response = mock(SecurePostResponse.class);
        when(response.isDelivered()).thenReturn(true);
        when(response.getTrackingId()).thenReturn("TRACK-123");

        when(securePostClient.send(
                any(SecurePostRequest.class),
                eq("securepost-client"),
                eq("securepost-secret-key")
        )).thenReturn(response);

        ProviderSendResult result = securePostProvider.sendMessage(createMessage(), CREDENTIALS_JSON);

        assertTrue(result.isSuccessful());
        assertEquals("SENT", result.getStatus());
        assertEquals("TRACK-123", result.getProviderMessageId());
        assertNull(result.getErrorMessage());

        verify(securePostClient).send(
                any(SecurePostRequest.class),
                eq("securepost-client"),
                eq("securepost-secret-key")
        );
    }

    @Test
    void sendMessageReturnsFailedWhenResponseIsNull() {
        when(securePostClient.send(
                any(SecurePostRequest.class),
                eq("securepost-client"),
                eq("securepost-secret-key")
        )).thenReturn(null);

        ProviderSendResult result = securePostProvider.sendMessage(createMessage(), CREDENTIALS_JSON);

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("SecurePost returned an empty response", result.getErrorMessage());
    }

    @Test
    void sendMessageReturnsFailedWhenNotDelivered() {
        SecurePostResponse response = mock(SecurePostResponse.class);
        when(response.isDelivered()).thenReturn(false);
        when(response.getErrorMessage()).thenReturn("Delivery failed");

        when(securePostClient.send(
                any(SecurePostRequest.class),
                eq("securepost-client"),
                eq("securepost-secret-key")
        )).thenReturn(response);

        ProviderSendResult result = securePostProvider.sendMessage(createMessage(), CREDENTIALS_JSON);

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("Delivery failed", result.getErrorMessage());
        assertNull(result.getProviderMessageId());
    }

    @Test
    void sendMessageReturnsFailedWhenClientThrowsException() {
        when(securePostClient.send(
                any(SecurePostRequest.class),
                eq("securepost-client"),
                eq("securepost-secret-key")
        )).thenThrow(new SecurePostApiException("SecurePost API error"));

        ProviderSendResult result = securePostProvider.sendMessage(createMessage(), CREDENTIALS_JSON);

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("SecurePost API error", result.getErrorMessage());
    }

    @Test
    void sendMessageReturnsFailedWhenCredentialsAreMissing() {
        ProviderSendResult result = securePostProvider.sendMessage(createMessage(), "{}");

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("SecurePost credentials missing clientId or clientSecret", result.getErrorMessage());

        verify(securePostClient, never()).send(any(), any(), any());
    }

    private NotificationQueueMessage createMessage() {
        return new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31612345678",
                "Test subject",
                "Test message",
                MessagingProviderType.SECUREPOST,
                "SMS",
                Instant.now()
        );
    }
}