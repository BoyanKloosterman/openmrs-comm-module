package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderSpec;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Zet herinneringen op RabbitMQ (US-001/002). */
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

    public int publishReminders(List<PolledAppointmentEntity> appointments, AppointmentReminderSpec spec) {
        return publishReminders(appointments, spec, null);
    }

    /** Optionele provider voor test-GUI; null = default uit scheduler-config. */
    public int publishReminders(
            List<PolledAppointmentEntity> appointments,
            AppointmentReminderSpec spec,
            MessagingProviderType providerOverride) {
        int queued = 0;
        for (PolledAppointmentEntity appointment : appointments) {
            if (!eligibilityService.maySendReminder(appointment)) {
                log.info(
                        "{}-herinnering overgeslagen voor {}: afspraak al begonnen of geannuleerd",
                        spec.logLabel(),
                        appointment.getAppointmentFhirId());
                continue;
            }
            if (deliveryLogService.hasSuccessfulDelivery(appointment.getAppointmentFhirId(), spec.messageType())) {
                log.debug(
                        "{}-herinnering overgeslagen voor {}: al eerder succesvol verstuurd",
                        spec.logLabel(),
                        appointment.getAppointmentFhirId());
                continue;
            }
            Optional<NotificationQueueMessage> messageOpt =
                    messageBuilder.buildReminder(appointment, spec);
            if (messageOpt.isEmpty()) {
                log.warn(
                        "Geen {}-herinnering voor appointment {}: ontbrekend telefoonnummer",
                        spec.logLabel(),
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
                    spec.logLabel(),
                    message.getNotificationId(),
                    appointment.getAppointmentFhirId(),
                    message.getRecipient());
        }
        return queued;
    }
}
