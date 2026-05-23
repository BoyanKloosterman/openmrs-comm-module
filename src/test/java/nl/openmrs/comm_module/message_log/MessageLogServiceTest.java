package nl.openmrs.comm_module.message_log;

import nl.openmrs.comm_module.messaging.queue.RabbitMqConsumer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.message_log.persistence.MessageLogRepository;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import nl.openmrs.comm_module.scheduling.NotificationScheduler;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class MessageLogServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T12:00:00Z");

    @MockitoBean
    @SuppressWarnings("unused")
    private OpenmrsFhirPollingService openmrsFhirPollingService;

    @MockitoBean
    @SuppressWarnings("unused")
    private NotificationScheduler notificationScheduler;

    @MockitoBean
    @SuppressWarnings("unused")
    private RabbitMqConsumer rabbitMqConsumer;

    @MockitoBean
    private Clock clock;

    @Autowired
    private MessageLogRepository repository;

    @Autowired
    private MessageLogService service;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void slaatProviderMetadataOp() {
        NotificationQueueMessage message = new NotificationQueueMessage();
        message.setNotificationId(UUID.randomUUID());
        message.setProvider(MessagingProviderType.SWIFTSEND);
        message.setMessageType("APPOINTMENT_REMINDER_24H");
        message.setQueuedAt(Instant.parse("2026-05-18T11:59:00Z"));
        message.setRetryCount(1);

        service.recordProviderAttempt(message, ProviderSendResult.success("prov-123"));

        assertEquals(1, repository.count());
        var entry = repository.findAll().get(0);
        assertEquals(message.getNotificationId(), entry.getNotificationId());
        assertEquals("SWIFTSEND", entry.getProvider());
        assertEquals("APPOINTMENT_REMINDER_24H", entry.getMessageType());
        assertEquals("SENT", entry.getStatus());
        assertEquals("prov-123", entry.getProviderMessageId());
        assertEquals(1, entry.getRetryCount());
        assertNotNull(entry.getSentAt());
    }
}
