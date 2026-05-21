package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/** US-001-4 / US-002-3: geen herinnering als afspraak al begonnen of geannuleerd. */
@Service
public class AppointmentReminderEligibilityService {

    private final Clock clock;

    public AppointmentReminderEligibilityService(Clock clock) {
        this.clock = clock;
    }

    public boolean maySendReminder(PolledAppointmentEntity appointment) {
        return maySendReminder(appointment, clock.instant());
    }

    public boolean maySendReminder(PolledAppointmentEntity appointment, Instant now) {
        if (appointment == null || appointment.isVoided()) {
            return false;
        }
        Instant start = appointment.getAppointmentDatetime();
        return start != null && start.isAfter(now);
    }
}
