package nl.openmrs.comm_module.message_log;

import nl.openmrs.comm_module.messaging.queue.RabbitMqConsumer;
import nl.openmrs.comm_module.message_log.persistence.MessageLogEntity;
import nl.openmrs.comm_module.message_log.persistence.MessageLogRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class MessageLogRetentionServiceTest {

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
    private MessageLogRetentionService retentionService;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void verwijdertLogsOuderDanBewaartermijn() {
        MessageLogEntity expired = sampleEntry(NOW.minus(366, ChronoUnit.DAYS));
        MessageLogEntity recent = sampleEntry(NOW.minus(10, ChronoUnit.DAYS));
        repository.save(expired);
        repository.save(recent);

        long removed = retentionService.purgeExpiredLogs();

        assertEquals(1, removed);
        assertEquals(1, repository.count());
    }

    private static MessageLogEntity sampleEntry(Instant sentAt) {
        MessageLogEntity entry = new MessageLogEntity();
        entry.setNotificationId(UUID.randomUUID());
        entry.setProvider("SWIFTSEND");
        entry.setMessageType("APPOINTMENT_REMINDER_24H");
        entry.setStatus("SENT");
        entry.setProviderMessageId("prov-1");
        entry.setSuccessful(true);
        entry.setQueuedAt(sentAt.minus(5, ChronoUnit.MINUTES));
        entry.setSentAt(sentAt);
        entry.setRetryCount(0);
        return entry;
    }
}
