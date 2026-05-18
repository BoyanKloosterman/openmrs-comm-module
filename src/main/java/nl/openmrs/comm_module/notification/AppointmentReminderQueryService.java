package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** US-001-2: ophalen welke polled encounters ~24 uur voor start in het venster vallen. */
@Service
public class AppointmentReminderQueryService {

    private final PolledEncounterRepository polledEncounterRepository;
    private final OpenmrsFhirProperties fhirProperties;
    private final NotificationSchedulerProperties schedulerProperties;
    private final Clock clock;

    public AppointmentReminderQueryService(
            PolledEncounterRepository polledEncounterRepository,
            OpenmrsFhirProperties fhirProperties,
            NotificationSchedulerProperties schedulerProperties,
            Clock clock) {
        this.polledEncounterRepository = polledEncounterRepository;
        this.fhirProperties = fhirProperties;
        this.schedulerProperties = schedulerProperties;
        this.clock = clock;
    }

    public List<PolledEncounterEntity> findEncountersDueFor24HourReminder() {
        Instant now = clock.instant();
        int leadHours = Math.max(0, schedulerProperties.getReminderLeadHours());
        int windowMinutes = Math.max(1, schedulerProperties.getReminderWindowMinutes());
        Duration halfWindow = Duration.ofMinutes(windowMinutes / 2L);

        Instant target = now.plus(Duration.ofHours(leadHours));
        Instant windowStart = target.minus(halfWindow);
        Instant windowEnd = target.plus(halfWindow);

        return polledEncounterRepository.findDueForReminderWindow(
                fhirProperties.getOrganisationId(), now, windowStart, windowEnd);
    }
}
