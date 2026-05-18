package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class NotificationDeliveryLogServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T12:00:00Z");

    @MockitoBean
    @SuppressWarnings("unused")
    private OpenmrsFhirPollingService openmrsFhirPollingService;

    @MockitoBean
    @SuppressWarnings("unused")
    private NotificationScheduler notificationScheduler;

    @MockitoBean
    private Clock clock;

    @Autowired
    private NotificationDeliveryLogRepository repository;

    @Autowired
    private NotificationDeliveryLogService deliveryLogService;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void slaatQueueEnProviderPogingOp() {
        NotificationQueueMessage message = sampleMessage();

        deliveryLogService.recordQueued(message);
        deliveryLogService.recordProviderAttempt(message, ProviderSendResult.success("prov-99"));

        assertEquals(2, repository.count());
        assertTrue(deliveryLogService.hasSuccessfulDelivery("enc-1", AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H));
    }

    @Test
    void misluktePogingNietAlsSuccesvol() {
        NotificationQueueMessage message = sampleMessage();
        deliveryLogService.recordProviderAttempt(message, ProviderSendResult.failed("timeout"));

        assertEquals(1, repository.count());
        assertFalse(deliveryLogService.hasSuccessfulDelivery("enc-1", AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H));
        assertEquals("FAILED", repository.findAll().get(0).getStatus());
    }

    private static NotificationQueueMessage sampleMessage() {
        NotificationQueueMessage message = new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31600000000",
                "Onderwerp",
                "Body",
                MessagingProviderType.SWIFTSEND,
                AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H,
                NOW);
        message.setEncounterFhirId("enc-1");
        return message;
    }
}
