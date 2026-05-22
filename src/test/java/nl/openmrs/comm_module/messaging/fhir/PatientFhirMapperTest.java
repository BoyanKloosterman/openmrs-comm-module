package nl.openmrs.comm_module.messaging.fhir;

import ca.uhn.fhir.context.FhirContext;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PatientFhirMapperTest {

  private PatientFhirMapper mapper;

  @Mock
  private FhirMessageValidator validator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mapper = new PatientFhirMapper(validator);

    // Default: alle valides resources zijn OK
    when(validator.validatePatientResource(any(Patient.class)))
        .thenReturn(FhirMessageValidationResult.valid());
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

  // US-009 Validation tests
  @Test
  void validatieFailureRetourneertLeegOptional() {
    Patient patient = new Patient();
    patient.setId("pat-invalid");
    patient.addName().setFamily("X");

    when(validator.validatePatientResource(patient))
        .thenReturn(FhirMessageValidationResult.invalid("Patient bevat geen telecom"));

    Optional<PatientPollDto> result = mapper.mapPatient(patient);
    assertTrue(result.isEmpty());
  }

  @Test
  void validatieSuccesResulteertInMapping() {
    Patient patient = new Patient();
    patient.setId("pat-valid");
    patient.addName().setFamily("Janssen");
    patient.addTelecom().setValue("+31612345678");

    when(validator.validatePatientResource(patient))
        .thenReturn(FhirMessageValidationResult.valid());

    Optional<PatientPollDto> result = mapper.mapPatient(patient);
    assertTrue(result.isPresent());
    assertEquals("pat-valid", result.get().patientId());
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
