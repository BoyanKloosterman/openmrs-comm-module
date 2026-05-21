package nl.openmrs.comm_module.notification.reminder;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppointmentReminderConfiguration {

    public static final String ID_24H = "24h";
    public static final String ID_1H = "1h";
    public static final String MESSAGE_TYPE_24H = "APPOINTMENT_REMINDER_24H";
    public static final String MESSAGE_TYPE_1H = "APPOINTMENT_REMINDER_1H";

    @Bean
    AppointmentReminderSpec appointmentReminder24Hours() {
        return new AppointmentReminderSpec(
                ID_24H,
                NotificationSchedulerProperties::getReminderLeadHours,
                MESSAGE_TYPE_24H,
                "24 uur",
                "24u");
    }

    @Bean
    AppointmentReminderSpec appointmentReminder1Hour() {
        return new AppointmentReminderSpec(
                ID_1H,
                NotificationSchedulerProperties::getReminder1LeadHours,
                MESSAGE_TYPE_1H,
                "1 uur",
                "1u");
    }

    @Bean
    List<AppointmentReminderSpec> appointmentReminderSpecs(
            AppointmentReminderSpec appointmentReminder24Hours,
            AppointmentReminderSpec appointmentReminder1Hour) {
        return List.of(appointmentReminder24Hours, appointmentReminder1Hour);
    }
}
