package nl.openmrs.comm_module.sync;

import org.hl7.fhir.r5.model.Appointment;

/** OpenMRS Appointment Scheduling status → FHIR R5 Appointment.status. */
public final class OpenmrsSchedulingStatusMapper {

    private OpenmrsSchedulingStatusMapper() {}

    public static Appointment.AppointmentStatus toFhir(String openmrsStatus, boolean voided) {
        if (voided) {
            return Appointment.AppointmentStatus.CANCELLED;
        }
        if (openmrsStatus == null || openmrsStatus.isBlank()) {
            return Appointment.AppointmentStatus.BOOKED;
        }
        return switch (openmrsStatus.trim().toUpperCase()) {
            case "SCHEDULED", "WALKIN", "WAITING" -> Appointment.AppointmentStatus.BOOKED;
            case "INCONSULTATION" -> Appointment.AppointmentStatus.ARRIVED;
            case "COMPLETED" -> Appointment.AppointmentStatus.FULFILLED;
            case "CANCELLED" -> Appointment.AppointmentStatus.CANCELLED;
            case "MISSED" -> Appointment.AppointmentStatus.NOSHOW;
            default -> Appointment.AppointmentStatus.BOOKED;
        };
    }
}
