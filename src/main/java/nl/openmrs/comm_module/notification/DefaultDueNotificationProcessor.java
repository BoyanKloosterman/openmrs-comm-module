package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Query (001-2) en queue (001-3) voor 24u-herinneringen. */
@Component
public class DefaultDueNotificationProcessor implements DueNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultDueNotificationProcessor.class);

    private final AppointmentReminderQueryService appointmentReminderQueryService;
    private final AppointmentReminderPublisher appointmentReminderPublisher;

    public DefaultDueNotificationProcessor(
            AppointmentReminderQueryService appointmentReminderQueryService,
            AppointmentReminderPublisher appointmentReminderPublisher) {
        this.appointmentReminderQueryService = appointmentReminderQueryService;
        this.appointmentReminderPublisher = appointmentReminderPublisher;
    }

    @Override
    public void processDueNotifications() {
        List<PolledEncounterEntity> due =
                appointmentReminderQueryService.findEncountersDueFor24HourReminder();
        int queued = appointmentReminderPublisher.publish24HourReminders(due);
        log.info("24u-herinnering: {} in venster, {} op queue gezet", due.size(), queued);
    }
}
