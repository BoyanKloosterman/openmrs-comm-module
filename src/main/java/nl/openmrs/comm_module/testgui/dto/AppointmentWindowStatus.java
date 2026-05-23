package nl.openmrs.comm_module.testgui.dto;

/** Status t.o.v. het geconfigureerde herinneringsvenster. */
public enum AppointmentWindowStatus {
    IN_REMINDER_WINDOW,
    TOO_EARLY,
    TOO_LATE,
    APPOINTMENT_PAST,
    VOIDED,
    MISSING_PHONE
}
