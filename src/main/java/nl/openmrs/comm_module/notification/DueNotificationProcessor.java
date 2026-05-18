package nl.openmrs.comm_module.notification;

/** Verwerkt notificaties die op dit moment verstuurd moeten worden (US-001-2+). */
public interface DueNotificationProcessor {

    void processDueNotifications();
}
