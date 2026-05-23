package nl.openmrs.comm_module.notification.content;

import java.time.ZonedDateTime;

/** Gestandaardiseerde velden voor herinneringsberichten (uitbreidbaar via provider). */
public record AppointmentNotificationContent(
        ZonedDateTime appointmentTime,
        String location,
        String instructions) {

    public static final String UNKNOWN_LOCATION = "nog niet bekend";

    public String locationOrDefault() {
        return location == null || location.isBlank() ? UNKNOWN_LOCATION : location.trim();
    }

    public boolean hasInstructions() {
        return instructions != null && !instructions.isBlank();
    }
}
