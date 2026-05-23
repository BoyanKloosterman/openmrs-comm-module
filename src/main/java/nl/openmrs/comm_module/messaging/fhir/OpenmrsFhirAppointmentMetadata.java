package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Annotation;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.StringType;

/** OpenMRS-specifieke metadata op FHIR Appointment (geen Location-resource nodig). */
public final class OpenmrsFhirAppointmentMetadata {

    /** Leesbare locatie (polikliniek/kamer) uit OpenMRS location.name. */
    public static final String EXT_LOCATION_DISPLAY =
            "http://openmrs.org/fhir/comm-module/location-display";

    /** OpenMRS appointment.reason (vrije instructies voor patiënt). */
    public static final String EXT_APPOINTMENT_REASON =
            "http://openmrs.org/fhir/comm-module/appointment-reason";

    private OpenmrsFhirAppointmentMetadata() {}

    public static void applyTo(Appointment appointment, String locationDisplay, String reason) {
        if (locationDisplay != null && !locationDisplay.isBlank()) {
            appointment.addExtension(
                    new Extension(EXT_LOCATION_DISPLAY, new StringType(locationDisplay.trim())));
        }
        if (reason != null && !reason.isBlank()) {
            String text = reason.trim();
            appointment.addExtension(new Extension(EXT_APPOINTMENT_REASON, new StringType(text)));
            appointment.addNote(new Annotation().setText(text));
        }
    }

    public static String readLocationDisplay(Appointment appointment) {
        return readStringExtension(appointment, EXT_LOCATION_DISPLAY);
    }

    /** Leest OpenMRS reason uit FHIR (extension of note). */
    public static String readReason(Appointment appointment) {
        String fromExt = readStringExtension(appointment, EXT_APPOINTMENT_REASON);
        if (fromExt != null) {
            return fromExt;
        }
        if (appointment.hasNote()) {
            for (Annotation note : appointment.getNote()) {
                if (note.hasText() && !note.getText().isBlank()) {
                    return note.getText().trim();
                }
            }
        }
        return null;
    }

    private static String readStringExtension(Appointment appointment, String url) {
        if (!appointment.hasExtension()) {
            return null;
        }
        for (Extension ext : appointment.getExtension()) {
            if (url.equals(ext.getUrl()) && ext.getValue() instanceof StringType st && st.hasValue()) {
                String v = st.getValue();
                return v == null || v.isBlank() ? null : v.trim();
            }
        }
        return null;
    }
}
