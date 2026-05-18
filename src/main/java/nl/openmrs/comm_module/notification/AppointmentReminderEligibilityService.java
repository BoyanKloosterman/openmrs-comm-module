package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
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

    public boolean maySend24HourReminder(PolledEncounterEntity encounter) {
        return maySend24HourReminder(encounter, clock.instant());
    }

    /**
     * Geen herinnering bij geannuleerde (voided) afspraak of als starttijd al is bereikt.
     */
    public boolean maySend24HourReminder(PolledEncounterEntity encounter, Instant now) {
        if (encounter == null || encounter.isVoided()) {
            return false;
        }
        Instant start = encounter.getEncounterDatetime();
        return start != null && start.isAfter(now);
    }
}
