package nl.openmrs.comm_module.sync;

import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenmrsFhirResourceFactoryTest {

    private OpenmrsFhirResourceFactory factory;
    private OpenmrsSchedulingSyncProperties properties;

    @BeforeEach
    void setUp() {
        factory = new OpenmrsFhirResourceFactory();
        properties = new OpenmrsSchedulingSyncProperties();
        properties.setZoneId("Europe/Amsterdam");
        properties.setFallbackPhone("+31612345678");
    }

    @Test
    void bouwtPatientMetFallbackTelefoon() {
        OpenmrsSchedulingAppointmentRow row = row(2, "SCHEDULED", false);
        Patient patient = factory.buildPatient(row, properties);
        assertEquals("omrs-patient-uuid-1", patient.getIdElement().getIdPart());
        assertTrue(patient.getTelecomFirstRep().hasValue());
    }

    @Test
    void zetStartInAmsterdamZoneNaarUtc() {
        OpenmrsSchedulingAppointmentRow row = new OpenmrsSchedulingAppointmentRow(
                2,
                "appt-uuid",
                "SCHEDULED",
                false,
                null,
                3,
                "uuid-1",
                "Boyan",
                "Kloosd",
                LocalDateTime.of(2026, 5, 20, 8, 0),
                LocalDateTime.of(2026, 5, 20, 9, 0),
                "Consult",
                "loc-uuid",
                "Unknown",
                null);

        Appointment appointment = factory.buildAppointment(row, properties);
        ZonedDateTime expected = ZonedDateTime.of(2026, 5, 20, 8, 0, 0, 0, ZoneId.of("Europe/Amsterdam"));
        assertEquals(expected.toInstant(), appointment.getStart().toInstant());
        assertEquals(Appointment.AppointmentStatus.BOOKED, appointment.getStatus());
    }

    private static OpenmrsSchedulingAppointmentRow row(int id, String status, boolean voided) {
        return new OpenmrsSchedulingAppointmentRow(
                id,
                "appt-uuid",
                status,
                voided,
                null,
                3,
                "uuid-1",
                "Boyan",
                "Kloosd",
                LocalDateTime.of(2026, 5, 20, 8, 0),
                LocalDateTime.of(2026, 5, 20, 9, 0),
                "Consult",
                "loc-uuid",
                "Unknown",
                null);
    }
}
