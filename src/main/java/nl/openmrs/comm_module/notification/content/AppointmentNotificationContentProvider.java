package nl.openmrs.comm_module.notification.content;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;

/** Bepaalt datum/tijd, locatie en instructies voor een polled afspraak. */
public interface AppointmentNotificationContentProvider {

    AppointmentNotificationContent resolve(PolledAppointmentEntity appointment);
}
