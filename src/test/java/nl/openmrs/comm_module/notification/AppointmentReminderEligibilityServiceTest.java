package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderEligibilityServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");

    @Mock
    private Clock clock;

    private AppointmentReminderEligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService = new AppointmentReminderEligibilityService(clock);
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void toekomstigeAfspraakMag() {
        PolledAppointmentEntity a = appointment(Instant.parse("2026-05-19T12:00:00Z"), false);
        assertTrue(eligibilityService.maySendReminder(a));
    }

    @Test
    void afspraakNetVoorNuMagNog() {
        PolledAppointmentEntity a = appointment(Instant.parse("2026-05-18T11:59:00Z"), false);
        assertTrue(eligibilityService.maySendReminder(a));
    }

    @Test
    void begonnenAfspraakMagNiet() {
        PolledAppointmentEntity a = appointment(NOW, false);
        assertFalse(eligibilityService.maySendReminder(a));
    }

    @Test
    void geannuleerdeAfspraakMagNiet() {
        PolledAppointmentEntity a = appointment(Instant.parse("2026-05-19T12:00:00Z"), true);
        assertFalse(eligibilityService.maySendReminder(a));
    }

    private static PolledAppointmentEntity appointment(Instant start, boolean voided) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setAppointmentFhirId("apt-1");
        a.setAppointmentDatetime(start);
        a.setVoided(voided);
        return a;
    }
}
