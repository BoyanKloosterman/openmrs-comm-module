package nl.openmrs.comm_module.messaging.fhir;

import ca.uhn.fhir.context.FhirContext;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientFhirMapperTest {

    private PatientFhirMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PatientFhirMapper();
    }

    @Test
    void maptMinimalePatientJson() throws Exception {
        Patient patient = parsePatientFixture("fhir/patient-minimaal.json");

        Optional<PatientPollDto> dto = mapper.mapPatient(patient);

        assertTrue(dto.isPresent());
        PatientPollDto p = dto.get();
        assertEquals("patient-test-1", p.patientId());
        assertEquals("Jan Jansen", p.displayName());
        assertEquals("+31612345678", p.phone());
    }

    @Test
    void zonderIdLeeg() {
        Patient patient = new Patient();
        patient.addName().setFamily("X");

        assertTrue(mapper.mapPatient(patient).isEmpty());
    }

    private static Patient parsePatientFixture(String classpathPath) throws Exception {
        FhirContext ctx = FhirContext.forR5();
        try (InputStream in = PatientFhirMapperTest.class.getClassLoader().getResourceAsStream(classpathPath)) {
            assertNotNull(in);
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return (Patient) ctx.newJsonParser().parseResource(json);
        }
    }
}
