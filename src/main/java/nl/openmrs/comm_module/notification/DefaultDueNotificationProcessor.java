package nl.openmrs.comm_module.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Placeholder tot US-001-2 query en verzending toevoegt. */
@Component
public class DefaultDueNotificationProcessor implements DueNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultDueNotificationProcessor.class);

    @Override
    public void processDueNotifications() {
        log.debug("Notificatie-check: nog geen due-query (US-001-2)");
    }
}
