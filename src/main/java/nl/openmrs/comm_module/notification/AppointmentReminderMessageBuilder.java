package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.notification.content.AppointmentNotificationContent;
import nl.openmrs.comm_module.notification.content.AppointmentNotificationContentProvider;
import nl.openmrs.comm_module.notification.content.AppointmentReminderBodyFormatter;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderSpec;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** US-001-3: queue-bericht via uitbreidbare content-provider en formatter. */
@Component
public class AppointmentReminderMessageBuilder {

    private final NotificationSchedulerProperties schedulerProperties;
    private final AppointmentNotificationContentProvider contentProvider;
    private final AppointmentReminderBodyFormatter bodyFormatter;

    public AppointmentReminderMessageBuilder(
            NotificationSchedulerProperties schedulerProperties,
            AppointmentNotificationContentProvider contentProvider,
            AppointmentReminderBodyFormatter bodyFormatter) {
        this.schedulerProperties = schedulerProperties;
        this.contentProvider = contentProvider;
        this.bodyFormatter = bodyFormatter;
    }

    public Optional<NotificationQueueMessage> buildReminder(
            PolledAppointmentEntity appointment, AppointmentReminderSpec spec) {
        return buildReminder(appointment, spec.leadLabel(), spec.messageType());
    }

    public AppointmentNotificationContent resolveContent(PolledAppointmentEntity appointment) {
        return contentProvider.resolve(appointment);
    }

    private Optional<NotificationQueueMessage> buildReminder(
            PolledAppointmentEntity appointment, String leadLabel, String messageType) {
        String phone = appointment.getPatientPhone();
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }

        AppointmentNotificationContent content = contentProvider.resolve(appointment);
        String greeting = resolveGreeting(appointment);
        String body = bodyFormatter.formatBody(greeting, leadLabel, content);

        NotificationQueueMessage message =
                new NotificationQueueMessage(
                        UUID.randomUUID(),
                        phone.trim(),
                        "Herinnering: afspraak over " + leadLabel,
                        body,
                        schedulerProperties.getDefaultProvider(),
                        messageType,
                        Instant.now());
        message.setAppointmentFhirId(appointment.getAppointmentFhirId());

        return Optional.of(message);
    }

    private static String resolveGreeting(PolledAppointmentEntity appointment) {
        String name = appointment.getPatientDisplayName();
        return (name != null && !name.isBlank()) ? "Beste " + name.trim() : "Beste patiënt";
    }
}
