package nl.openmrs.comm_module.provider.asyncflow;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsyncFlowProviderTest {

    private AsyncFlowClient asyncFlowClient;
    private AsyncFlowProvider asyncFlowProvider;

    @BeforeEach
    void setUp() {
        asyncFlowClient = mock(AsyncFlowClient.class);
        asyncFlowProvider = new AsyncFlowProvider(asyncFlowClient);
    }

    @Test
    void getTypeReturnsAsyncFlow() {
        assertEquals(MessagingProviderType.ASYNCFLOW, asyncFlowProvider.getType());
    }

    @Test
    void sendMessageReturnsSubmittedWhenMessageIsAccepted() {
        AsyncFlowSubmitResponse response = mock(AsyncFlowSubmitResponse.class);
        when(response.isAccepted()).thenReturn(true);
        when(response.getTrackingId()).thenReturn("ASF-1234567890ABCDEF");

        when(asyncFlowClient.submit(any(AsyncFlowSubmitRequest.class))).thenReturn(response);

        ProviderSendResult result = asyncFlowProvider.sendMessage(createMessage());

        assertTrue(result.isSuccessful());
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals("ASF-1234567890ABCDEF", result.getProviderMessageId());
        assertNull(result.getErrorMessage());

        verify(asyncFlowClient).submit(any(AsyncFlowSubmitRequest.class));
    }

    @Test
    void sendMessageReturnsFailedWhenResponseIsNull() {
        when(asyncFlowClient.submit(any(AsyncFlowSubmitRequest.class))).thenReturn(null);

        ProviderSendResult result = asyncFlowProvider.sendMessage(createMessage());

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("AsyncFlow returned an empty response", result.getErrorMessage());
    }

    @Test
    void sendMessageReturnsFailedWhenMessageIsNotAccepted() {
        AsyncFlowSubmitResponse response = mock(AsyncFlowSubmitResponse.class);
        when(response.isAccepted()).thenReturn(false);
        when(response.getMessage()).thenReturn("Message was rejected");

        when(asyncFlowClient.submit(any(AsyncFlowSubmitRequest.class))).thenReturn(response);

        ProviderSendResult result = asyncFlowProvider.sendMessage(createMessage());

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("Message was rejected", result.getErrorMessage());
        assertNull(result.getProviderMessageId());
    }

    @Test
    void sendMessageReturnsFailedWhenClientThrowsException() {
        when(asyncFlowClient.submit(any(AsyncFlowSubmitRequest.class)))
                .thenThrow(new AsyncFlowApiException("AsyncFlow API error"));

        ProviderSendResult result = asyncFlowProvider.sendMessage(createMessage());

        assertFalse(result.isSuccessful());
        assertEquals("FAILED", result.getStatus());
        assertEquals("AsyncFlow API error", result.getErrorMessage());
    }

    private NotificationQueueMessage createMessage() {
        return new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31612345678",
                "Test subject",
                "Test message",
                MessagingProviderType.ASYNCFLOW,
                "SMS",
                Instant.now()
        );
    }
}