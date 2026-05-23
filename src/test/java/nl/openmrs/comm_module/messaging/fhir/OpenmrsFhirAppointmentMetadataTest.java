package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Appointment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenmrsFhirAppointmentMetadataTest {

    @Test
    void roundtripLocatieEnInstructies() {
        Appointment appointment = new Appointment();
        appointment.setId("appt-1");

        OpenmrsFhirAppointmentMetadata.applyTo(
                appointment, "Polikliniek Interne - Kamer 4", "Nuchter blijven, neem medicijnen mee.");

        assertEquals("Polikliniek Interne - Kamer 4", OpenmrsFhirAppointmentMetadata.readLocationDisplay(appointment));
        assertEquals(
                "Nuchter blijven, neem medicijnen mee.",
                OpenmrsFhirAppointmentMetadata.readReason(appointment));
    }

    @Test
    void leestInstructiesUitNoteAlsGeenExtension() {
        Appointment appointment = new Appointment();
        appointment.addNote().setText("Alleen via note");

        assertNull(OpenmrsFhirAppointmentMetadata.readLocationDisplay(appointment));
        assertEquals("Alleen via note", OpenmrsFhirAppointmentMetadata.readReason(appointment));
    }
}
