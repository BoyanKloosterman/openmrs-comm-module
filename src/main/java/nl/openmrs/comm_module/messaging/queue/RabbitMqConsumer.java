package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.NotificationDeliveryLogService;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.provider.MessagingProvider;
import nl.openmrs.comm_module.provider.MessagingProviderFactory;
import nl.openmrs.comm_module.provider.ProviderSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConsumer.class);

    private final MessagingProviderFactory providerFactory;
    private final NotificationDeliveryLogService deliveryLogService;
    private final RabbitMqProducer rabbitMqProducer;
    private final PolledAppointmentRepository polledAppointmentRepository;
    private final OpenmrsFhirProperties fhirProperties;
    private final int maxAttempts;

    public RabbitMqConsumer(
            MessagingProviderFactory providerFactory,
            NotificationDeliveryLogService deliveryLogService,
            RabbitMqProducer rabbitMqProducer,
            PolledAppointmentRepository polledAppointmentRepository,
            OpenmrsFhirProperties fhirProperties,
            @Value("${messaging.retry.max-attempts}") int maxAttempts) {
        this.providerFactory = providerFactory;
        this.deliveryLogService = deliveryLogService;
        this.rabbitMqProducer = rabbitMqProducer;
        this.polledAppointmentRepository = polledAppointmentRepository;
        this.fhirProperties = fhirProperties;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = "#{'${messaging.queues}'.split(',')}")
    public void consume(NotificationQueueMessage message) {
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
        ProviderSendResult result = provider.sendMessage(message);
        deliveryLogService.recordProviderAttempt(message, result);

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
                .map(a -> a.isVoided())
                .orElse(false);
    }

    private void handleFailedMessage(NotificationQueueMessage message, ProviderSendResult result) {
        if (message.getRetryCount() < maxAttempts) {
            message.incrementRetryCount();
            log.warn(
                    "Retry notificatie notificationId={} poging={} van {}: {}",
                    message.getNotificationId(),
                    message.getRetryCount(),
                    maxAttempts,
                    result.getErrorMessage());
            rabbitMqProducer.publishRetry(message);
            return;
        }

        log.error(
                "Max retry bereikt voor notificationId={}: {}",
                message.getNotificationId(),
                result.getErrorMessage());
        throw new AmqpRejectAndDontRequeueException(
                "Notification failed after "
                        + maxAttempts
                        + " retry attempts. Last error: "
                        + result.getErrorMessage());
    }
}
