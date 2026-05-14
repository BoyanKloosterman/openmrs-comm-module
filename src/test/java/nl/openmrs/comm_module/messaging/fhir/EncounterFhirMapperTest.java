package nl.openmrs.comm_module.messaging.fhir;

import ca.uhn.fhir.context.FhirContext;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterPollDto;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterFhirMapperTest {

    private EncounterFhirMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EncounterFhirMapper();
    }

    @Test
    void maptMinimaleEncounterJsonNaarDto() throws Exception {
        Encounter encounter = parseEncounterFixture("fhir/encounter-minimaal.json");

        Optional<EncounterPollDto> dto = mapper.mapEncounterToEncounterPollDto(encounter);

        assertTrue(dto.isPresent());
        EncounterPollDto e = dto.get();
        assertEquals("test-encounter-uuid", e.uuid());
        assertEquals("test-encounter-uuid", e.encounterId());
        assertEquals("patient-42", e.patientId());
        assertEquals(Instant.parse("2026-05-14T10:15:30Z"), e.encounterDatetime());
        assertEquals("ward-7", e.locationId());
        assertEquals("Follow-up visit", e.encounterType());
        assertFalse(e.voided());
    }

    @Test
    void cancelledEncounterIsVoided() {
        Encounter encounter = new Encounter();
        encounter.setId("enc-void");
        encounter.setStatus(Encounter.EncounterStatus.CANCELLED);
        encounter.getSubject().setReference("Patient/p1");
        encounter.getPeriod().getStartElement().setValueAsString("2026-01-01");

        Optional<EncounterPollDto> dto = mapper.mapEncounterToEncounterPollDto(encounter);

        assertTrue(dto.isPresent());
        assertTrue(dto.get().voided());
    }

    @Test
    void zonderPatientLeeg() {
        Encounter encounter = new Encounter();
        encounter.setId("x");
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.getPeriod().getStartElement().setValueAsString("2026-01-01");

        assertTrue(mapper.mapEncounterToEncounterPollDto(encounter).isEmpty());
    }

    @Test
    void zonderPeriodStartLeeg() {
        Encounter encounter = new Encounter();
        encounter.setId("y");
        encounter.setStatus(Encounter.EncounterStatus.PLANNED);
        encounter.getSubject().setReference("Patient/p1");

        assertTrue(mapper.mapEncounterToEncounterPollDto(encounter).isEmpty());
    }

    private static Encounter parseEncounterFixture(String classpathPath) throws Exception {
        FhirContext ctx = FhirContext.forR4();
        try (InputStream in = EncounterFhirMapperTest.class.getClassLoader().getResourceAsStream(classpathPath)) {
            assertNotNull(in);
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return (Encounter) ctx.newJsonParser().parseResource(json);
        }
    }
}
