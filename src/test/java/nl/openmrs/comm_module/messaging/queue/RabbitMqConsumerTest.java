package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.organisation.service.OrganisationConfigService;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMqConsumerTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final String ORGANISATION_ID = "org-test";
    private static final String CREDENTIALS_JSON = "{\"apiKey\":\"test-swiftsend-key\"}";

    @Mock
    private MessagingProviderFactory providerFactory;

    @Mock
    private MessagingProvider messagingProvider;

    @Mock
    private NotificationDeliveryLogService deliveryLogService;

    @Mock
    private RabbitMqProducer rabbitMqProducer;

    @Mock
    private PolledAppointmentRepository polledAppointmentRepository;

    @Mock
    private OpenmrsFhirProperties fhirProperties;

    @Mock
    private OrganisationConfigService organisationConfigService;

    private RabbitMqConsumer consumer;

    @BeforeEach
    void setUp() {
        lenient().when(fhirProperties.getOrganisationId()).thenReturn(ORGANISATION_ID);

        consumer =
                new RabbitMqConsumer(
                        providerFactory,
                        deliveryLogService,
                        rabbitMqProducer,
                        polledAppointmentRepository,
                        fhirProperties,
                        organisationConfigService,
                        MAX_ATTEMPTS);
    }

    @Test
    void logtVerzendstatusNaProviderPoging() {
        NotificationQueueMessage message = createMessage();
        ProviderSendResult result = ProviderSendResult.success("ext-1");

        when(deliveryLogService.hasQueuedDeliveryRecord(message.getNotificationId())).thenReturn(true);
        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(messagingProvider);
        when(organisationConfigService.getDecryptedCredentials(ORGANISATION_ID, MessagingProviderType.SWIFTSEND))
                .thenReturn(CREDENTIALS_JSON);
        when(messagingProvider.sendMessage(message, CREDENTIALS_JSON)).thenReturn(result);

        consumer.consume(message);

        verify(messagingProvider).sendMessage(message, CREDENTIALS_JSON);
        verify(deliveryLogService).recordProviderAttempt(message, result);
        verify(rabbitMqProducer, never()).publishRetry(message);
    }

    @Test
    void plantRetryBijMisluktePoging() {
        NotificationQueueMessage message = createMessage();
        ProviderSendResult result = ProviderSendResult.failed("timeout");

        when(deliveryLogService.hasQueuedDeliveryRecord(message.getNotificationId())).thenReturn(true);
        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(messagingProvider);
        when(organisationConfigService.getDecryptedCredentials(ORGANISATION_ID, MessagingProviderType.SWIFTSEND))
                .thenReturn(CREDENTIALS_JSON);
        when(messagingProvider.sendMessage(message, CREDENTIALS_JSON)).thenReturn(result);

        consumer.consume(message);

        verify(deliveryLogService).recordProviderAttempt(message, result);
        verify(rabbitMqProducer).publishRetry(message);
    }

    @Test
    void slaatVerzendingOverBijGeannuleerdeAfspraak() {
        NotificationQueueMessage message = createMessage();
        message.setAppointmentFhirId("apt-voided");

        PolledAppointmentEntity voided = new PolledAppointmentEntity();
        voided.setVoided(true);
        voided.setAppointmentDatetime(Instant.parse("2026-05-20T10:00:00Z"));

        when(deliveryLogService.hasQueuedDeliveryRecord(message.getNotificationId())).thenReturn(true);
        when(polledAppointmentRepository.findByOrganisationIdAndAppointmentFhirId("org-test", "apt-voided"))
                .thenReturn(Optional.of(voided));

        consumer.consume(message);

        verify(providerFactory, never()).getProvider(any());
        verify(organisationConfigService, never()).getDecryptedCredentials(any(), any());
        verify(deliveryLogService, never()).recordProviderAttempt(any(), any());
    }

    @Test
    void weigertNaMaxRetriesAlsGeenVolgendeProviderBestaat() {
        NotificationQueueMessage message = createMessage();
        message.setRetryCount(MAX_ATTEMPTS);

        ProviderSendResult result = ProviderSendResult.failed("timeout");

        when(deliveryLogService.hasQueuedDeliveryRecord(message.getNotificationId())).thenReturn(true);
        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(messagingProvider);
        when(organisationConfigService.getDecryptedCredentials(ORGANISATION_ID, MessagingProviderType.SWIFTSEND))
                .thenReturn(CREDENTIALS_JSON);
        when(messagingProvider.sendMessage(message, CREDENTIALS_JSON)).thenReturn(result);
        when(organisationConfigService.getEnabledProviders(ORGANISATION_ID)).thenReturn(java.util.List.of());

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consume(message));

        verify(rabbitMqProducer, never()).publishRetry(message);
    }

    @Test
    void weigertZonderRequeueBijOntbrekendeOrganisatieconfig() {
        NotificationQueueMessage message = createMessage();

        when(deliveryLogService.hasQueuedDeliveryRecord(message.getNotificationId())).thenReturn(true);
        when(providerFactory.getProvider(MessagingProviderType.SWIFTSEND)).thenReturn(messagingProvider);
        when(organisationConfigService.getDecryptedCredentials(ORGANISATION_ID, MessagingProviderType.SWIFTSEND))
                .thenThrow(new IllegalArgumentException("Organisation config not found: " + ORGANISATION_ID));

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consume(message));

        verify(deliveryLogService)
                .recordProviderAttempt(eq(message), org.mockito.ArgumentMatchers.argThat(r -> !r.isSuccessful()));
        verify(messagingProvider, never()).sendMessage(any(), any());
        verify(rabbitMqProducer, never()).publishRetry(message);
    }

    @Test
    void slaatVerzendingOverAlsQueuedBijAnnuleringWegIs() {
        NotificationQueueMessage message = createMessage();
        message.setAppointmentFhirId("omrs-appt-24");

        when(deliveryLogService.hasQueuedDeliveryRecord(message.getNotificationId())).thenReturn(false);

        consumer.consume(message);

        verify(providerFactory, never()).getProvider(any());
        verify(organisationConfigService, never()).getDecryptedCredentials(any(), any());
        verify(deliveryLogService, never()).recordProviderAttempt(any(), any());
    }

    private NotificationQueueMessage createMessage() {
        NotificationQueueMessage message = new NotificationQueueMessage();
        message.setNotificationId(UUID.randomUUID());
        message.setProvider(MessagingProviderType.SWIFTSEND);
        message.setOrganisationId(ORGANISATION_ID);
        return message;
    }
}