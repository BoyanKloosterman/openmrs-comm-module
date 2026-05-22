package nl.openmrs.comm_module.notification.reminder;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;

/** Vaste specs voor unit tests zonder Spring-context. */
public final class AppointmentReminderTestSpecs {

    public static final AppointmentReminderSpec HOURS_24 =
            new AppointmentReminderSpec(
                    AppointmentReminderConfiguration.ID_24H,
                    NotificationSchedulerProperties::getReminderLeadHours,
                    AppointmentReminderConfiguration.MESSAGE_TYPE_24H,
                    "24 uur",
                    "24u");

    public static final AppointmentReminderSpec HOURS_1 =
            new AppointmentReminderSpec(
                    AppointmentReminderConfiguration.ID_1H,
                    NotificationSchedulerProperties::getReminder1LeadHours,
                    AppointmentReminderConfiguration.MESSAGE_TYPE_1H,
                    "1 uur",
                    "1u");

    private AppointmentReminderTestSpecs() {}
}
