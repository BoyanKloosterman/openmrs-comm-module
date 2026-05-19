package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @MockitoBean
    private OpenmrsFhirOperations fhirOperations;

    @Autowired
    private PolledAppointmentRepository polledAppointmentRepository;

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
        when(fhirOperations.readAppointmentByLogicalId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void processorZetAppointmentIn24uVensterOpQueue() {
        saveDueAppointment("apt-flow", "+31611112222");

        dueNotificationProcessor.processDueNotifications();

        ArgumentCaptor<NotificationQueueMessage> captor = ArgumentCaptor.forClass(NotificationQueueMessage.class);
        verify(rabbitMqProducer).publish(captor.capture());
        NotificationQueueMessage msg = captor.getValue();
        assertEquals("apt-flow", msg.getAppointmentFhirId());
        assertEquals(AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H, msg.getMessageType());
        assertEquals(1, deliveryLogRepository.count());
        assertEquals(NotificationDeliveryLogService.STATUS_QUEUED, deliveryLogRepository.findAll().get(0).getStatus());
    }

    @Test
    void tweedeSchedulerTickQueueNietOpnieuwNaEerstePoging() {
        saveDueAppointment("apt-dedup", "+31633334444");
        schedulerProperties.setEnabled(true);

        notificationScheduler.checkDueNotifications();
        notificationScheduler.checkDueNotifications();

        verify(rabbitMqProducer, times(1)).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void schedulerTickMetUitgeschakeldeSchedulerQueueNiets() {
        schedulerProperties.setEnabled(false);
        saveDueAppointment("apt-off", "+31655556666");

        notificationScheduler.checkDueNotifications();

        verify(rabbitMqProducer, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private void saveDueAppointment(String appointmentFhirId, String phone) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setOrganisationId("test-org");
        a.setAppointmentUuid("uuid-" + appointmentFhirId);
        a.setAppointmentFhirId(appointmentFhirId);
        a.setPatientFhirId("pat-" + appointmentFhirId);
        a.setAppointmentDatetime(Instant.parse("2026-05-19T10:05:00Z"));
        a.setPatientPhone(phone);
        a.setPatientDisplayName("Test");
        a.setLocationId("poli-1");
        a.setVoided(false);
        a.setLastPolledAt(NOW);
        polledAppointmentRepository.save(a);
    }
}
