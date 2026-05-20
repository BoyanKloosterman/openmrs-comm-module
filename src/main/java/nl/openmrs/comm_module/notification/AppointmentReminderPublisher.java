package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** Zet 24u-herinneringen op RabbitMQ (US-001-3); US-001-4 check vlak voor queue. */
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
        int queued = 0;
        for (PolledAppointmentEntity appointment : appointments) {
            if (!eligibilityService.maySend24HourReminder(appointment)) {
                log.info(
                        "24u-herinnering overgeslagen voor {}: afspraak al begonnen of geannuleerd",
                        appointment.getAppointmentFhirId());
                continue;
            }
            if (deliveryLogService.hasSuccessfulDelivery(
                    appointment.getAppointmentFhirId(), AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H)) {
                log.debug(
                        "24u-herinnering overgeslagen voor {}: al eerder succesvol verstuurd",
                        appointment.getAppointmentFhirId());
                continue;
            }
            var messageOpt = messageBuilder.build24HourReminder(appointment);
            if (messageOpt.isEmpty()) {
                log.warn(
                        "Geen herinnering voor appointment {}: ontbrekend telefoonnummer",
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
                    "24u-herinnering in queue: notificationId={} appointment={} naar {}",
                    message.getNotificationId(),
                    appointment.getAppointmentFhirId(),
                    message.getRecipient());
        }
        return queued;
    }
}
