package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Roept US-001-2 query aan; verzending volgt in US-001-3. */
@Component
public class DefaultDueNotificationProcessor implements DueNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultDueNotificationProcessor.class);

    private final AppointmentReminderQueryService appointmentReminderQueryService;

    public DefaultDueNotificationProcessor(AppointmentReminderQueryService appointmentReminderQueryService) {
        this.appointmentReminderQueryService = appointmentReminderQueryService;
    }

    @Override
    public void processDueNotifications() {
        List<PolledEncounterEntity> due =
                appointmentReminderQueryService.findEncountersDueFor24HourReminder();
        log.info("24u-herinnering: {} encounter(s) in venster", due.size());
        if (log.isDebugEnabled()) {
            due.forEach(e -> log.debug(
                    "due encounter fhirId={} at {}", e.getEncounterFhirId(), e.getEncounterDatetime()));
        }
    }
}
