package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** Zet 24u- en 1u-herinneringen op RabbitMQ (US-001-3 / US-002). */
@Service
public class AppointmentReminderPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderPublisher.class);

    private final AppointmentReminderMessageBuilder messageBuilder;
    private final AppointmentReminderEligibilityService eligibilityService;
    private final NotificationDeliveryLogService deliveryLogService;
    private final RabbitMqProducer rabbitMqProducer;

    public AppointmentReminderPublisher(
            AppointmentReminderMessageBuilder messageBuilder,
            AppointmentReminderEligibilityService eligibilityService,
            NotificationDeliveryLogService deliveryLogService,
            RabbitMqProducer rabbitMqProducer) {
        this.messageBuilder = messageBuilder;
        this.eligibilityService = eligibilityService;
        this.deliveryLogService = deliveryLogService;
        this.rabbitMqProducer = rabbitMqProducer;
    }

    public int publish24HourReminders(List<PolledAppointmentEntity> appointments) {
        return publish24HourReminders(appointments, null);
    }

    /** Optionele provider voor test-GUI; null = default uit scheduler-config. */
    public int publish24HourReminders(
            List<PolledAppointmentEntity> appointments, MessagingProviderType providerOverride) {
        return publishReminders(
                appointments,
                providerOverride,
                AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H,
                "24u",
                messageBuilder::build24HourReminder);
    }

    public int publish1HourReminders(List<PolledAppointmentEntity> appointments) {
        return publish1HourReminders(appointments, null);
    }

    /** Optionele provider voor test-GUI; null = default uit scheduler-config. */
    public int publish1HourReminders(
            List<PolledAppointmentEntity> appointments, MessagingProviderType providerOverride) {
        return publishReminders(
                appointments,
                providerOverride,
                AppointmentReminderMessageBuilder.MESSAGE_TYPE_1H,
                "1u",
                messageBuilder::build1HourReminder);
    }

    private int publishReminders(
            List<PolledAppointmentEntity> appointments,
            MessagingProviderType providerOverride,
            String messageType,
            String logLabel,
            java.util.function.Function<PolledAppointmentEntity, java.util.Optional<NotificationQueueMessage>>
                    buildMessage) {
        int queued = 0;
        for (PolledAppointmentEntity appointment : appointments) {
            if (!eligibilityService.maySendReminder(appointment)) {
                log.info(
                        "{}-herinnering overgeslagen voor {}: afspraak al begonnen of geannuleerd",
                        logLabel,
                        appointment.getAppointmentFhirId());
                continue;
            }
            if (deliveryLogService.hasSuccessfulDelivery(appointment.getAppointmentFhirId(), messageType)) {
                log.debug(
                        "{}-herinnering overgeslagen voor {}: al eerder succesvol verstuurd",
                        logLabel,
                        appointment.getAppointmentFhirId());
                continue;
            }
            var messageOpt = buildMessage.apply(appointment);
            if (messageOpt.isEmpty()) {
                log.warn(
                        "Geen {}-herinnering voor appointment {}: ontbrekend telefoonnummer",
                        logLabel,
                        appointment.getAppointmentFhirId());
                continue;
            }
            NotificationQueueMessage message = messageOpt.get();
            if (providerOverride != null) {
                message.setProvider(providerOverride);
            }
            rabbitMqProducer.publish(message);
            deliveryLogService.recordQueued(message);
            queued++;
            log.info(
                    "{}-herinnering in queue: notificationId={} appointment={} naar {}",
                    logLabel,
                    message.getNotificationId(),
                    appointment.getAppointmentFhirId(),
                    message.getRecipient());
        }
        return queued;
    }
}
