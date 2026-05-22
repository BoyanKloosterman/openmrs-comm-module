package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderSpec;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** US-001/002: appointments in herinneringsvenster voor een gegeven spec. */
@Service
public class AppointmentReminderQueryService {

    private final PolledAppointmentRepository polledAppointmentRepository;
    private final OpenmrsFhirProperties fhirProperties;
    private final NotificationSchedulerProperties schedulerProperties;
    private final AppointmentReminderEligibilityService eligibilityService;
    private final Clock clock;

    public AppointmentReminderQueryService(
            PolledAppointmentRepository polledAppointmentRepository,
            OpenmrsFhirProperties fhirProperties,
            NotificationSchedulerProperties schedulerProperties,
            AppointmentReminderEligibilityService eligibilityService,
            Clock clock) {
        this.polledAppointmentRepository = polledAppointmentRepository;
        this.fhirProperties = fhirProperties;
        this.schedulerProperties = schedulerProperties;
        this.eligibilityService = eligibilityService;
        this.clock = clock;
    }

    public List<PolledAppointmentEntity> findAppointmentsDueFor(AppointmentReminderSpec spec) {
        return findAppointmentsDueForReminder(spec.leadHours(schedulerProperties));
    }

    public List<PolledAppointmentEntity> findAppointmentsDueForReminder(int leadHours) {
        Instant now = clock.instant();
        int windowMinutes = Math.max(1, schedulerProperties.getReminderWindowMinutes());
        Duration halfWindow = Duration.ofMinutes(windowMinutes / 2L);

        Instant target = now.plus(Duration.ofHours(leadHours));
        Instant windowStart = target.minus(halfWindow);
        Instant windowEnd = target.plus(halfWindow);

        return polledAppointmentRepository
                .findDueForReminderWindow(
                        fhirProperties.getOrganisationId(), now, windowStart, windowEnd)
                .stream()
                .filter(a -> eligibilityService.maySendReminder(a, now))
                .toList();
    }
}
