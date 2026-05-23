package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.notification.voided.VoidedAppointmentHandler;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** US-017: geplande notificaties verwijderen + log bij late annulering. */
@Service
public class CancelledAppointmentNotificationService implements VoidedAppointmentHandler {

    private static final Logger log = LoggerFactory.getLogger(CancelledAppointmentNotificationService.class);

    private final NotificationDeliveryLogService deliveryLogService;

    public CancelledAppointmentNotificationService(NotificationDeliveryLogService deliveryLogService) {
        this.deliveryLogService = deliveryLogService;
    }

    @Override
    public void handleAfterPoll(PolledAppointmentEntity appointment, boolean wasVoidedBefore) {
        if (appointment == null || !appointment.isVoided()) {
            return;
        }
        String appointmentFhirId = appointment.getAppointmentFhirId();
        int removed = deliveryLogService.cancelQueuedNotifications(appointmentFhirId);
        if (removed > 0) {
            log.info(
                    "Geannuleerde afspraak {}: {} geplande notificatie(s) verwijderd",
                    appointmentFhirId,
                    removed);
        }
        if (!wasVoidedBefore && deliveryLogService.hasAnySuccessfulDelivery(appointmentFhirId)) {
            log.warn(
                    "Afspraak {} geannuleerd nadat herinnering al verstuurd was",
                    appointmentFhirId);
        }
    }
}
