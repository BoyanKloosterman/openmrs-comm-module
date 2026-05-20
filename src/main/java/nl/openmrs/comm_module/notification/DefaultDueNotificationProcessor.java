package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** US-001/002: 24u- en 1u-herinneringen in één scheduler-tick. */
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
        List<PolledAppointmentEntity> due24 =
                appointmentReminderQueryService.findAppointmentsDueFor24HourReminder();
        int queued24 = appointmentReminderPublisher.publish24HourReminders(due24);

        List<PolledAppointmentEntity> due1 =
                appointmentReminderQueryService.findAppointmentsDueFor1HourReminder();
        int queued1 = appointmentReminderPublisher.publish1HourReminders(due1);

        log.info(
                "Herinneringen: 24u {} in venster / {} queue; 1u {} in venster / {} queue",
                due24.size(),
                queued24,
                due1.size(),
                queued1);
    }
}
