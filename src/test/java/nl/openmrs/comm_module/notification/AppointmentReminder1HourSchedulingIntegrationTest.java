package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.queue.RabbitMqConsumer;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
            "comm.notification.scheduler.reminder-1-lead-hours=1",
            "comm.notification.scheduler.reminder-window-minutes=60",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
class AppointmentReminder1HourSchedulingIntegrationTest {

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
    private AppointmentReminderPublisher appointmentReminderPublisher;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(fhirOperations.readAppointmentByLogicalId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void processorZetAppointmentIn1uVensterOpQueue() {
        saveDueAppointment("apt-1h", "+31611112222", Instant.parse("2026-05-18T11:05:00Z"));

        dueNotificationProcessor.processDueNotifications();

        ArgumentCaptor<NotificationQueueMessage> captor = ArgumentCaptor.forClass(NotificationQueueMessage.class);
        verify(rabbitMqProducer, org.mockito.Mockito.atLeastOnce()).publish(captor.capture());
        assertTrue(
                captor.getAllValues().stream()
                        .anyMatch(m -> AppointmentReminderMessageBuilder.MESSAGE_TYPE_1H.equals(m.getMessageType())));
    }

    @Test
    void eenUurWordtVerstuurdOokAls24uAlSuccesvol() {
        PolledAppointmentEntity apt = saveDueAppointment("apt-both", "+31699998888", Instant.parse("2026-05-18T11:05:00Z"));
        appointmentReminderPublisher.publish24HourReminders(List.of(apt));

        int queued1 = appointmentReminderPublisher.publish1HourReminders(List.of(apt));

        assertEquals(1, queued1);
        long count1h =
                deliveryLogRepository.findAll().stream()
                        .filter(e -> AppointmentReminderMessageBuilder.MESSAGE_TYPE_1H.equals(e.getMessageType()))
                        .count();
        assertTrue(count1h >= 1);
    }

    private PolledAppointmentEntity saveDueAppointment(
            String appointmentFhirId, String phone, Instant appointmentDatetime) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setOrganisationId("test-org");
        a.setAppointmentUuid("uuid-" + appointmentFhirId);
        a.setAppointmentFhirId(appointmentFhirId);
        a.setPatientFhirId("pat-" + appointmentFhirId);
        a.setAppointmentDatetime(appointmentDatetime);
        a.setPatientPhone(phone);
        a.setPatientDisplayName("Test");
        a.setLocationId("poli-1");
        a.setVoided(false);
        a.setLastPolledAt(NOW);
        return polledAppointmentRepository.save(a);
    }
}
