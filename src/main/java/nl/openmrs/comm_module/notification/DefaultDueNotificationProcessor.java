package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.notification.reminder.AppointmentReminderSpec;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** US-001/002: verwerkt alle geregistreerde herinneringsspecs (OCP). */
@Component
public class DefaultDueNotificationProcessor implements DueNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultDueNotificationProcessor.class);

    private final List<AppointmentReminderSpec> reminderSpecs;
    private final AppointmentReminderQueryService appointmentReminderQueryService;
    private final AppointmentReminderPublisher appointmentReminderPublisher;

    public DefaultDueNotificationProcessor(
            List<AppointmentReminderSpec> reminderSpecs,
            AppointmentReminderQueryService appointmentReminderQueryService,
            AppointmentReminderPublisher appointmentReminderPublisher) {
        this.reminderSpecs = List.copyOf(reminderSpecs);
        this.appointmentReminderQueryService = appointmentReminderQueryService;
        this.appointmentReminderPublisher = appointmentReminderPublisher;
    }

    @Override
    public void processDueNotifications() {
        StringBuilder summary = new StringBuilder("Herinneringen:");
        for (AppointmentReminderSpec spec : reminderSpecs) {
            List<PolledAppointmentEntity> due = appointmentReminderQueryService.findAppointmentsDueFor(spec);
            int queued = appointmentReminderPublisher.publishReminders(due, spec);
            summary
                    .append(' ')
                    .append(spec.logLabel())
                    .append(' ')
                    .append(due.size())
                    .append(" in venster / ")
                    .append(queued)
                    .append(" queue;");
        }
        log.info(summary.toString());
    }
}
