package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.testgui.dto.AppointmentWindowStatus;
import nl.openmrs.comm_module.testgui.dto.ReminderWindowDto;

import java.time.Duration;
import java.time.Instant;

/** Bepaalt 24u/1u-status t.o.v. het verzendmoment (afspraakstart − lead), niet de scheduler-queryband. */
final class AppointmentReminderWindowStatusResolver {

    private AppointmentReminderWindowStatusResolver() {}

    static AppointmentWindowStatus resolve(
            PolledAppointmentEntity a, Instant now, ReminderWindowDto window) {
        if (a.isVoided()) {
            return AppointmentWindowStatus.VOIDED;
        }
        Instant start = a.getAppointmentDatetime();
        if (start == null || !start.isAfter(now)) {
            return AppointmentWindowStatus.APPOINTMENT_PAST;
        }
        if (a.getPatientPhone() == null || a.getPatientPhone().isBlank()) {
            return AppointmentWindowStatus.MISSING_PHONE;
        }
        Duration halfWindow = Duration.ofMinutes(Math.max(1, window.windowMinutes()) / 2L);
        Instant sendAt = start.minus(Duration.ofHours(Math.max(0, window.leadHours())));
        Instant sendWindowStart = sendAt.minus(halfWindow);
        Instant sendWindowEnd = sendAt.plus(halfWindow);

        if (now.isBefore(sendWindowStart)) {
            return AppointmentWindowStatus.TOO_EARLY;
        }
        if (!now.isBefore(sendWindowEnd)) {
            return AppointmentWindowStatus.TOO_LATE;
        }
        return AppointmentWindowStatus.IN_REMINDER_WINDOW;
    }
}
