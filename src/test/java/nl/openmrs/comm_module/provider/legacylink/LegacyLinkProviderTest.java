package nl.openmrs.comm_module.provider.legacylink;

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

class LegacyLinkProviderTest {

    private static final String CREDENTIALS_JSON =
            "{\"username\":\"legacylink-user\",\"password\":\"legacylink-password\"}";

    private LegacyLinkClient legacyLinkClient;
    private LegacyLinkProvider legacyLinkProvider;

    @BeforeEach
    void setUp() {
        legacyLinkClient = mock(LegacyLinkClient.class);
        legacyLinkProvider = new LegacyLinkProvider(legacyLinkClient, "OpenMRS");
    }

    @Test
    void getTypeReturnsLegacyLink() {
        assertEquals(MessagingProviderType.LEGACYLINK, legacyLinkProvider.getType());
    }

    @Test
    void sendMessageReturnsSuccessWhenSoapResponseIsSuccessful() {
        LegacyLinkSoapResponse response = new LegacyLinkSoapResponse(
                200,
                "SMS sent successfully",
                "LGC-1234567890ABCDEF",
                "2026-05-18T14:00:00Z"
        );

        when(legacyLinkClient.sendSms(
                any(LegacyLinkSoapRequest.class),
                eq("legacylink-user"),
                eq("legacylink-password")
        )).thenReturn(response);

        ProviderSendResult result = legacyLinkProvider.sendMessage(createMessage(), CREDENTIALS_JSON);

        assertTrue(result.isSuccessful());
        assertEquals("SENT", result.getStatus());
        assertEquals("LGC-1234567890ABCDEF", result.getProviderMessageId());
        assertNull(result.getErrorMessage());

        verify(legacyLinkClient).sendSms(
                any(LegacyLinkSoapRequest.class),
                eq("legacylink-user"),
                eq("legacylink-password")
        );
    }

    @Test
    void sendMessageReturnsFailedWhenSoapResponseIsNotSuccessful() {
        LegacyLinkSoapResponse response = new LegacyLinkSoapResponse(
                401,
                "Authentication required",
                "",
                "2026-05-18T14:00:00Z"
        );

        when(legacyLinkClient.sendSms(
                any(LegacyLinkSoapRequest.class),
                eq("legacylink-user"),
                eq("legacylink-password")
        )).thenReturn(response);

        ProviderSendResult result = legacyLinkProvider.sendMessage(createMessage(), CREDENTIALS_JSON);

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertNull(result.getProviderMessageId());
        assertTrue(result.getErrorMessage().contains("LegacyLink failed"));
        assertTrue(result.getErrorMessage().contains("401"));
    }

    @Test
    void sendMessageReturnsFailedWhenClientThrowsException() {
        when(legacyLinkClient.sendSms(
                any(LegacyLinkSoapRequest.class),
                eq("legacylink-user"),
                eq("legacylink-password")
        )).thenThrow(new LegacyLinkApiException("LegacyLink request failed"));

        ProviderSendResult result = legacyLinkProvider.sendMessage(createMessage(), CREDENTIALS_JSON);

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("LegacyLink request failed", result.getErrorMessage());
    }

    @Test
    void sendMessageReturnsFailedWhenCredentialsAreMissing() {
        ProviderSendResult result = legacyLinkProvider.sendMessage(createMessage(), "{}");

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("LegacyLink credentials missing username or password", result.getErrorMessage());

        verify(legacyLinkClient, never()).sendSms(any(), any(), any());
    }

    private NotificationQueueMessage createMessage() {
        return new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31612345678",
                "Test subject",
                "Test message",
                MessagingProviderType.LEGACYLINK,
                "SMS",
                Instant.now()
        );
    }
}