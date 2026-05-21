package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqConsumer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.persistence.NotificationDeliveryLogRepository;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderConfiguration;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderTestSpecs;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.scheduling.NotificationScheduler;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@TestPropertySource(
        properties = {
            "comm.notification.scheduler.enabled=false",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
class AppointmentCancellationIntegrationTest {

    private static final String ORG = "test-org";
    private static final Instant START = Instant.parse("2026-05-22T14:00:00Z");

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
    private AppointmentPollPersistence pollPersistence;

    @Autowired
    private PolledAppointmentRepository polledAppointmentRepository;

    @Autowired
    private NotificationDeliveryLogService deliveryLogService;

    @Autowired
    private NotificationDeliveryLogRepository deliveryLogRepository;

    @Autowired
    private AppointmentReminderQueryService reminderQueryService;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-05-21T14:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void pollNaarVoidedVerwijdertQueuedEnSluitHerinneringenUit() {
        AppointmentPollDto active =
                new AppointmentPollDto("uuid-1", "apt-cancel", "pat-1", START, "loc", "consult", null, false);
        pollPersistence.upsertPollResults(ORG, List.of(new AppointmentWithPatientDto(active, null)));
        assertEquals(
                1,
                reminderQueryService.findAppointmentsDueFor(AppointmentReminderTestSpecs.HOURS_24).size());

        NotificationQueueMessage queued = queuedMessage("apt-cancel");
        deliveryLogService.recordQueued(queued);
        assertEquals(1, deliveryLogRepository.count());

        AppointmentPollDto cancelled =
                new AppointmentPollDto("uuid-1", "apt-cancel", "pat-1", START, "loc", "consult", null, true);
        pollPersistence.upsertPollResults(ORG, List.of(new AppointmentWithPatientDto(cancelled, null)));

        PolledAppointmentEntity stored =
                polledAppointmentRepository
                        .findByOrganisationIdAndAppointmentFhirId(ORG, "apt-cancel")
                        .orElseThrow();
        assertTrue(stored.isVoided());
        assertTrue(
                deliveryLogRepository
                        .findByAppointmentFhirIdAndStatus("apt-cancel", NotificationDeliveryLogService.STATUS_QUEUED)
                        .isEmpty());
        assertTrue(
                reminderQueryService
                        .findAppointmentsDueFor(AppointmentReminderTestSpecs.HOURS_24)
                        .isEmpty());
    }

    @Test
    void pollNaarVoidedBehoudtSuccesvolleVerzending() {
        AppointmentPollDto active =
                new AppointmentPollDto("uuid-2", "apt-sent", "pat-1", START, "loc", "consult", null, false);
        pollPersistence.upsertPollResults(ORG, List.of(new AppointmentWithPatientDto(active, null)));

        NotificationQueueMessage sent = queuedMessage("apt-sent");
        deliveryLogService.recordQueued(sent);
        deliveryLogService.recordProviderAttempt(sent, nl.openmrs.comm_module.provider.ProviderSendResult.success("ext-1"));

        AppointmentPollDto cancelled =
                new AppointmentPollDto("uuid-2", "apt-sent", "pat-1", START, "loc", "consult", null, true);
        pollPersistence.upsertPollResults(ORG, List.of(new AppointmentWithPatientDto(cancelled, null)));

        assertTrue(deliveryLogService.hasAnySuccessfulDelivery("apt-sent"));
        assertEquals(1, deliveryLogRepository.findAll().stream().filter(e -> e.isSuccessful()).count());
    }

    private static NotificationQueueMessage queuedMessage(String appointmentFhirId) {
        NotificationQueueMessage message =
                new NotificationQueueMessage(
                        UUID.randomUUID(),
                        "+31600000000",
                        "Onderwerp",
                        "Body",
                        MessagingProviderType.SWIFTSEND,
                        AppointmentReminderConfiguration.MESSAGE_TYPE_24H,
                        Instant.parse("2026-05-21T12:00:00Z"));
        message.setAppointmentFhirId(appointmentFhirId);
        return message;
    }
}
