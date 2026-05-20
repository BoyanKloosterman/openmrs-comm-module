package nl.openmrs.comm_module.sync;

import org.hl7.fhir.r5.model.Appointment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenmrsSchedulingStatusMapperTest {

    @Test
    void scheduledWordtBooked() {
        assertEquals(Appointment.AppointmentStatus.BOOKED, OpenmrsSchedulingStatusMapper.toFhir("SCHEDULED", false));
    }

    @Test
    void voidedWordtCancelled() {
        assertEquals(Appointment.AppointmentStatus.CANCELLED, OpenmrsSchedulingStatusMapper.toFhir("SCHEDULED", true));
    }

    @Test
    void missedWordtNoshow() {
        assertEquals(Appointment.AppointmentStatus.NOSHOW, OpenmrsSchedulingStatusMapper.toFhir("MISSED", false));
    }
}
