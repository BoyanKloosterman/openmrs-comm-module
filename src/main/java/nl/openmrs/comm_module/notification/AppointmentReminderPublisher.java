package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
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
    private final RabbitMqProducer rabbitMqProducer;

    public AppointmentReminderPublisher(
            AppointmentReminderMessageBuilder messageBuilder,
            AppointmentReminderEligibilityService eligibilityService,
            RabbitMqProducer rabbitMqProducer) {
        this.messageBuilder = messageBuilder;
        this.eligibilityService = eligibilityService;
        this.rabbitMqProducer = rabbitMqProducer;
    }

    /** Aantal berichten dat daadwerkelijk op de queue is gezet. */
    public int publish24HourReminders(List<PolledEncounterEntity> encounters) {
        int queued = 0;
        for (PolledEncounterEntity encounter : encounters) {
            if (!eligibilityService.maySend24HourReminder(encounter)) {
                log.info(
                        "24u-herinnering overgeslagen voor {}: afspraak al begonnen of geannuleerd",
                        encounter.getEncounterFhirId());
                continue;
            }
            var messageOpt = messageBuilder.build24HourReminder(encounter);
            if (messageOpt.isEmpty()) {
                log.warn(
                        "Geen herinnering voor encounter {}: ontbrekend telefoonnummer",
                        encounter.getEncounterFhirId());
                continue;
            }
            NotificationQueueMessage message = messageOpt.get();
            rabbitMqProducer.publish(message);
            queued++;
            log.info(
                    "24u-herinnering in queue: notificationId={} encounter={} naar {}",
                    message.getNotificationId(),
                    encounter.getEncounterFhirId(),
                    message.getRecipient());
        }
        return queued;
    }
}
