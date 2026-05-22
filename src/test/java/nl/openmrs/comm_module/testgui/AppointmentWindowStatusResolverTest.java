package nl.openmrs.comm_module.testgui;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.testgui.dto.AppointmentWindowStatus;
import nl.openmrs.comm_module.testgui.dto.ReminderWindowDto;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentWindowStatusResolverTest {

    @Test
    void afspraakOver24uHeeft1uTeVroegEn24uInVenster() {
        Instant now = Instant.parse("2026-05-22T11:00:00Z");
        PolledAppointmentEntity a = appointmentAt(now.plus(Duration.ofHours(24)));

        assertEquals(
                AppointmentWindowStatus.IN_REMINDER_WINDOW,
                AppointmentReminderWindowStatusResolver.resolve(a, now, window(now, 24, 60)));
        assertEquals(
                AppointmentWindowStatus.TOO_EARLY,
                AppointmentReminderWindowStatusResolver.resolve(a, now, window(now, 1, 60)));
    }

    private static PolledAppointmentEntity appointmentAt(Instant start) {
        PolledAppointmentEntity e = new PolledAppointmentEntity();
        e.setAppointmentDatetime(start);
        e.setPatientPhone("+31612345678");
        e.setVoided(false);
        return e;
    }

    private static ReminderWindowDto window(Instant now, int leadHours, int windowMinutes) {
        Duration half = Duration.ofMinutes(windowMinutes / 2L);
        Instant target = now.plus(Duration.ofHours(leadHours));
        return new ReminderWindowDto(
                target, target.minus(half), target.plus(half), leadHours, windowMinutes);
    }
}
