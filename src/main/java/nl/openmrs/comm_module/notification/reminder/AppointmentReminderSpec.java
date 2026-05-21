package nl.openmrs.comm_module.notification.reminder;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;

import java.util.function.ToIntFunction;

/** Eén herinneringstype: lead time, message type en labels (OCP: nieuw type = nieuwe spec-bean). */
public record AppointmentReminderSpec(
        String id,
        ToIntFunction<NotificationSchedulerProperties> leadHoursResolver,
        String messageType,
        String leadLabel,
        String logLabel) {

    public int leadHours(NotificationSchedulerProperties properties) {
        return Math.max(0, leadHoursResolver.applyAsInt(properties));
    }
}
