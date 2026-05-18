package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.notification.DueNotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** US-001-1: periodiek controleren welke notificaties verstuurd moeten worden. */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationSchedulerProperties properties;
    private final DueNotificationProcessor dueNotificationProcessor;

    public NotificationScheduler(
            NotificationSchedulerProperties properties, DueNotificationProcessor dueNotificationProcessor) {
        this.properties = properties;
        this.dueNotificationProcessor = dueNotificationProcessor;
    }

    @Scheduled(fixedDelayString = "#{@notificationSchedulerProperties.checkDelayMillis()}")
    public void checkDueNotifications() {
        if (!properties.isEnabled()) {
            return;
        }
        log.debug("Notificatie-scheduler tick");
        try {
            dueNotificationProcessor.processDueNotifications();
        } catch (RuntimeException e) {
            // Scheduler mag niet crashen bij fouten in latere US-001-taken
            log.error("Notificatie-check mislukt: {}", e.getMessage(), e);
        }
    }
}
