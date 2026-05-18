package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterRepository;
import nl.openmrs.comm_module.messaging.queue.RabbitMqConsumer;
import nl.openmrs.comm_module.scheduling.NotificationScheduler;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-001-6: integratietest van de volledige 24u-schedulingketen (001-1 t/m 001-5).
 * Unit-tests per component staan in de bijbehorende *Test-klassen.
 */
@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(
        properties = {
            "comm.notification.scheduler.enabled=false",
            "openmrs.fhir.organisation-id=test-org",
            "comm.notification.scheduler.reminder-lead-hours=24",
            "comm.notification.scheduler.reminder-window-minutes=60",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
class AppointmentReminderSchedulingIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");

    @MockitoBean
    @SuppressWarnings("unused")
    private OpenmrsFhirPollingService openmrsFhirPollingService;

    @MockitoBean
    private RabbitMqProducer rabbitMqProducer;

    @MockitoBean
    @SuppressWarnings("unused")
    private RabbitMqConsumer rabbitMqConsumer;

    @MockitoBean
    private Clock clock;

    @Autowired
    private PolledEncounterRepository polledEncounterRepository;

    @Autowired
    private NotificationDeliveryLogRepository deliveryLogRepository;

    @Autowired
    private DefaultDueNotificationProcessor dueNotificationProcessor;

    @Autowired
    private NotificationScheduler notificationScheduler;

    @Autowired
    private NotificationSchedulerProperties schedulerProperties;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void processorZetEncounterIn24uVensterOpQueue() {
        saveDueEncounter("enc-flow", "+31611112222");

        dueNotificationProcessor.processDueNotifications();

        ArgumentCaptor<NotificationQueueMessage> captor = ArgumentCaptor.forClass(NotificationQueueMessage.class);
        verify(rabbitMqProducer).publish(captor.capture());
        NotificationQueueMessage msg = captor.getValue();
        assertEquals("enc-flow", msg.getEncounterFhirId());
        assertEquals(AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H, msg.getMessageType());
        assertEquals(1, deliveryLogRepository.count());
        assertEquals(NotificationDeliveryLogService.STATUS_QUEUED, deliveryLogRepository.findAll().get(0).getStatus());
    }

    @Test
    void tweedeSchedulerTickQueueNietOpnieuwNaEerstePoging() {
        saveDueEncounter("enc-dedup", "+31633334444");
        schedulerProperties.setEnabled(true);

        notificationScheduler.checkDueNotifications();
        notificationScheduler.checkDueNotifications();

        verify(rabbitMqProducer, times(1)).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void schedulerTickMetUitgeschakeldeSchedulerQueueNiets() {
        schedulerProperties.setEnabled(false);
        saveDueEncounter("enc-off", "+31655556666");

        notificationScheduler.checkDueNotifications();

        verify(rabbitMqProducer, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private void saveDueEncounter(String encounterFhirId, String phone) {
        PolledEncounterEntity e = new PolledEncounterEntity();
        e.setOrganisationId("test-org");
        e.setEncounterUuid("uuid-" + encounterFhirId);
        e.setEncounterFhirId(encounterFhirId);
        e.setPatientFhirId("pat-" + encounterFhirId);
        e.setEncounterDatetime(Instant.parse("2026-05-19T10:05:00Z"));
        e.setPatientPhone(phone);
        e.setPatientDisplayName("Test");
        e.setLocationId("poli-1");
        e.setVoided(false);
        e.setLastPolledAt(NOW);
        polledEncounterRepository.save(e);
    }
}
