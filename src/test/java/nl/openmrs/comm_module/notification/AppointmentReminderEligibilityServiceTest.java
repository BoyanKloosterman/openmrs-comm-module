package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderEligibilityServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T12:00:00Z");

    @Mock
    private Clock clock;

    private AppointmentReminderEligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService = new AppointmentReminderEligibilityService(clock);
    }

    @Test
    void toegestaanAlsAfspraakInToekomst() {
        when(clock.instant()).thenReturn(NOW);
        PolledEncounterEntity e = encounter(Instant.parse("2026-05-19T12:00:00Z"), false);
        assertTrue(eligibilityService.maySend24HourReminder(e));
    }

    @Test
    void geweigerdAlsAfspraakAlBegonnen() {
        when(clock.instant()).thenReturn(NOW);
        PolledEncounterEntity e = encounter(Instant.parse("2026-05-18T11:59:00Z"), false);
        assertFalse(eligibilityService.maySend24HourReminder(e));
    }

    @Test
    void geweigerdOpExactStartmoment() {
        when(clock.instant()).thenReturn(NOW);
        PolledEncounterEntity e = encounter(NOW, false);
        assertFalse(eligibilityService.maySend24HourReminder(e));
    }

    @Test
    void geweigerdAlsVoided() {
        when(clock.instant()).thenReturn(NOW);
        PolledEncounterEntity e = encounter(Instant.parse("2026-05-19T12:00:00Z"), true);
        assertFalse(eligibilityService.maySend24HourReminder(e));
    }

    private static PolledEncounterEntity encounter(Instant start, boolean voided) {
        PolledEncounterEntity e = new PolledEncounterEntity();
        e.setEncounterFhirId("enc-1");
        e.setEncounterDatetime(start);
        e.setVoided(voided);
        return e;
    }
}
