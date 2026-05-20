package nl.openmrs.comm_module.sync;

import java.time.LocalDateTime;

/** Eén rij uit OpenMRS appointmentscheduling_* (JDBC). */
public record OpenmrsSchedulingAppointmentRow(
        int appointmentId,
        String appointmentUuid,
        String status,
        boolean voided,
        LocalDateTime dateChanged,
        int patientId,
        String patientUuid,
        String givenName,
        String familyName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String appointmentTypeName,
        String locationUuid,
        String locationName,
        String reason,
        String phone) {

    public String syncToken() {
        return status
                + "|"
                + voided
                + "|"
                + startDate
                + "|"
                + endDate
                + "|"
                + nullToEmpty(locationName)
                + "|"
                + nullToEmpty(reason);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    public String fhirPatientId() {
        return "omrs-patient-" + patientUuid;
    }

    public String fhirAppointmentId() {
        return "omrs-appt-" + appointmentId;
    }
}
