package nl.openmrs.comm_module.notification.voided;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;

/** US-017: reactie op geannuleerde afspraak na poll (OCP: nieuwe handler = nieuwe bean). */
public interface VoidedAppointmentHandler {

    void handleAfterPoll(PolledAppointmentEntity appointment, boolean wasVoidedBefore);
}
