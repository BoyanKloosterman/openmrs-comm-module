package nl.openmrs.comm_module.testgui;

import java.util.Optional;

/** Alleen legacy sync-ids (omrs-appt-{n}); SPA gebruikt appointment-uuid. */
final class OpenmrsLegacyAppointmentIds {

    private OpenmrsLegacyAppointmentIds() {}

    static Optional<Integer> resolveOpenmrsId(String appointmentFhirId) {
        if (appointmentFhirId == null || !appointmentFhirId.startsWith("omrs-appt-")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(appointmentFhirId.substring("omrs-appt-".length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
