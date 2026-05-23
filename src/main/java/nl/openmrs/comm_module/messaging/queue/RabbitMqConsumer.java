package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.metrics.MessagingMetrics;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.message_log.MessageLogService;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.organisation.dto.OrganisationProviderConfigResponse;
import nl.openmrs.comm_module.organisation.service.OrganisationConfigService;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RabbitMqConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConsumer.class);

    private final MessagingProviderFactory providerFactory;
    private final NotificationDeliveryLogService deliveryLogService;
    private final MessageLogService messageLogService;
    private final MessagingMetrics messagingMetrics;
    private final RabbitMqProducer rabbitMqProducer;
    private final PolledAppointmentRepository polledAppointmentRepository;
    private final OpenmrsFhirProperties fhirProperties;
    private final int maxAttempts;

    private final OrganisationConfigService organisationConfigService;

    public RabbitMqConsumer(
            MessagingProviderFactory providerFactory,
            NotificationDeliveryLogService deliveryLogService,
            MessageLogService messageLogService,
            MessagingMetrics messagingMetrics,
            RabbitMqProducer rabbitMqProducer,
            PolledAppointmentRepository polledAppointmentRepository,
            OpenmrsFhirProperties fhirProperties,
            OrganisationConfigService organisationConfigService,
            @Value("${messaging.retry.max-attempts}") int maxAttempts) {
        this.providerFactory = providerFactory;
        this.deliveryLogService = deliveryLogService;
        this.messageLogService = messageLogService;
        this.messagingMetrics = messagingMetrics;
        this.rabbitMqProducer = rabbitMqProducer;
        this.polledAppointmentRepository = polledAppointmentRepository;
        this.fhirProperties = fhirProperties;
        this.organisationConfigService = organisationConfigService;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = "#{'${messaging.queues}'.split(',')}")
    public void consume(NotificationQueueMessage message) {
        messagingMetrics.recordDequeued(message);

        if (!deliveryLogService.hasQueuedDeliveryRecord(message.getNotificationId())) {
            log.info(
                    "Notificatie overgeslagen (geen QUEUED meer): appointment={} notificationId={}",
                    message.getAppointmentFhirId(),
                    message.getNotificationId());
            return;
        }
        if (isVoidedAppointment(message)) {
            log.info(
                    "Notificatie overgeslagen voor geannuleerde afspraak {}: notificationId={}",
                    message.getAppointmentFhirId(),
                    message.getNotificationId());
            return;
        }
        MessagingProvider provider = providerFactory.getProvider(message.getProvider());

        String credentialsJson;
        try {
            credentialsJson = organisationConfigService.getDecryptedCredentials(
                    message.getOrganisationId(),
                    message.getProvider());
        } catch (IllegalArgumentException e) {
            String error = e.getMessage();
            log.error(
                    "Notificatie afgebroken (geen organisatieconfig): notificationId={} org={} provider={}: {}",
                    message.getNotificationId(),
                    message.getOrganisationId(),
                    message.getProvider(),
                    error);
            ProviderSendResult configFailure = ProviderSendResult.failed(error);
            deliveryLogService.recordProviderAttempt(message, configFailure);
            messagingMetrics.recordSendResult(message, configFailure);
            throw new AmqpRejectAndDontRequeueException(error, e);
        }
        ProviderSendResult result = provider.sendMessage(message, credentialsJson);
        deliveryLogService.recordProviderAttempt(message, result);
        messageLogService.recordProviderAttempt(message, result);
        messagingMetrics.recordSendResult(message, result);

        if (!result.isSuccessful()) {
            handleFailedMessage(message, result);
        }
    }

    private boolean isVoidedAppointment(NotificationQueueMessage message) {
        String appointmentFhirId = message.getAppointmentFhirId();
        if (appointmentFhirId == null || appointmentFhirId.isBlank()) {
            return false;
        }
        return polledAppointmentRepository
                .findByOrganisationIdAndAppointmentFhirId(
                        fhirProperties.getOrganisationId(), appointmentFhirId)
                .map(PolledAppointmentEntity::isVoided)
                .orElse(false);
    }

    private void handleFailedMessage(NotificationQueueMessage message, ProviderSendResult result) {
        if (message.getRetryCount() < maxAttempts) {
            message.incrementRetryCount();

            log.warn(
                    "Retry notificatie notificationId={} provider={} poging={} van {}: {}",
                    message.getNotificationId(),
                    message.getProvider(),
                    message.getRetryCount(),
                    maxAttempts,
                    result.getErrorMessage());

            rabbitMqProducer.publishRetry(message);
            return;
        }

        if (moveToNextProvider(message)) {
            log.warn(
                    "Max retries bereikt voor notificationId={} provider={}, switch naar provider={}",
                    message.getNotificationId(),
                    message.getProvider(),
                    message.getProvider());

            rabbitMqProducer.publish(message);
            return;
        }

        log.error(
                "Alle providers gefaald voor notificationId={}. Laatste provider={} fout={}",
                message.getNotificationId(),
                message.getProvider(),
                result.getErrorMessage());

        throw new AmqpRejectAndDontRequeueException(
                "Notification failed after all configured providers. Last error: "
                        + result.getErrorMessage());
    }

    private boolean moveToNextProvider(NotificationQueueMessage message) {
        List<MessagingProviderType> providerChain = getProviderChain(message);

        if (providerChain.isEmpty()) {
            return false;
        }

        int currentIndex = providerChain.indexOf(message.getProvider());
        int nextIndex = currentIndex < 0 ? 0 : currentIndex + 1;

        if (nextIndex >= providerChain.size()) {
            return false;
        }

        MessagingProviderType nextProvider = providerChain.get(nextIndex);

        message.setProvider(nextProvider);
        message.setProviderAttemptIndex(nextIndex);
        message.setRetryCount(0);

        return true;
    }

    private List<MessagingProviderType> getProviderChain(NotificationQueueMessage message) {
        if (message.getOrganisationId() == null || message.getOrganisationId().isBlank()) {
            return List.of(message.getProvider());
        }

        return organisationConfigService
                .getEnabledProviders(message.getOrganisationId())
                .stream()
                .map(OrganisationProviderConfigResponse::getProviderType)
                .toList();
    }

}
