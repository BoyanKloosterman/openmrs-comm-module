package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/** US-001-4: bepaalt of een 24u-herinnering nog verstuurd mag worden. */
@Service
public class AppointmentReminderEligibilityService {

    private final Clock clock;

    public AppointmentReminderEligibilityService(Clock clock) {
        this.clock = clock;
    }

    public boolean maySend24HourReminder(PolledAppointmentEntity appointment) {
        return maySend24HourReminder(appointment, clock.instant());
    }

    public boolean maySend24HourReminder(PolledAppointmentEntity appointment, Instant now) {
        if (appointment == null || appointment.isVoided()) {
            return false;
        }
        Instant start = appointment.getAppointmentDatetime();
        return start != null && start.isAfter(now);
    }
}
