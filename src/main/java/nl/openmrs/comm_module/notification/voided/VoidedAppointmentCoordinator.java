package nl.openmrs.comm_module.notification.voided;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/** Roept alle voided-handlers aan (SRP: alleen coördinatie). */
@Component
public class VoidedAppointmentCoordinator {

    private final List<VoidedAppointmentHandler> handlers;

    public VoidedAppointmentCoordinator(List<VoidedAppointmentHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public void notifyIfVoided(PolledAppointmentEntity appointment, boolean wasVoidedBefore) {
        if (appointment == null || !appointment.isVoided()) {
            return;
        }
        for (VoidedAppointmentHandler handler : handlers) {
            handler.handleAfterPoll(appointment, wasVoidedBefore);
        }
    }
}
