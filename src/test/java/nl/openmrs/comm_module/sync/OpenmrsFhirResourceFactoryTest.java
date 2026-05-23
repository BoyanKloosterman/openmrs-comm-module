package nl.openmrs.comm_module.sync;

import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
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
        properties.setDbZoneId("UTC");
        properties.setFallbackPhone("+31612345678");
    }

    @Test
    void bouwtPatientMetFallbackTelefoon() {
        OpenmrsSchedulingAppointmentRow row = row(2, "SCHEDULED", false);
        Patient patient = factory.buildPatient(row, properties);
        assertEquals("uuid-1", patient.getIdElement().getIdPart());
        assertTrue(patient.getTelecomFirstRep().hasValue());
    }

    @Test
    void zetStartViaDbZoneNaarInstant() {
        // SPA reference distro: 13:05 in DB (UTC) = 15:05 in NL — niet als Amsterdam lezen.
        LocalDateTime dbStart = LocalDateTime.of(2026, 5, 23, 13, 5);
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
                dbStart,
                LocalDateTime.of(2026, 5, 23, 14, 5),
                "Consult",
                "loc-uuid",
                "Unknown",
                null,
                null);

        Appointment appointment = factory.buildAppointment(row, properties);
        ZonedDateTime expectedUtc = dbStart.atZone(ZoneId.of("UTC"));
        assertEquals(expectedUtc.toInstant(), appointment.getStart().toInstant());
        assertEquals(
                "15:05",
                expectedUtc
                        .withZoneSameInstant(ZoneId.of("Europe/Amsterdam"))
                        .toLocalTime()
                        .toString());
        assertEquals(Appointment.AppointmentStatus.BOOKED, appointment.getStatus());
    }

    @Test
    void exporteertLocatieEnReasonNaarFhirMetadata() {
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
                "Polikliniek Hart - Kamer 2",
                "Nuchter blijven",
                null);

        Appointment appointment = factory.buildAppointment(row, properties);

        assertEquals(
                "Polikliniek Hart - Kamer 2",
                OpenmrsFhirAppointmentMetadata.readLocationDisplay(appointment));
        assertEquals("Nuchter blijven", OpenmrsFhirAppointmentMetadata.readReason(appointment));
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
                null,
                null);
    }
}
