package nl.openmrs.comm_module.config;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenmrsSchedulingSyncPropertiesTest {

    @Test
    void patientAppointmentDbUtcGeeftAmsterdamWeergave() {
        OpenmrsSchedulingSyncProperties props = new OpenmrsSchedulingSyncProperties();
        props.setSource("patient-appointment");
        props.setDbZoneId("UTC");
        props.setZoneId("Europe/Amsterdam");

        LocalDateTime dbWallClock = LocalDateTime.of(2026, 5, 23, 11, 3);
        Instant instant = dbWallClock.atZone(props.effectiveDbZoneId()).toInstant();

        String inAmsterdam =
                instant.atZone(ZoneId.of(props.getZoneId())).toLocalTime().toString();

        assertEquals("13:03", inAmsterdam);
    }
}
