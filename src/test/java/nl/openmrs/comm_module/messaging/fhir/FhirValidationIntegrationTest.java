package nl.openmrs.comm_module.messaging.fhir;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.sync.OpenmrsFhirResourceFactory;
import nl.openmrs.comm_module.sync.OpenmrsSchedulingAppointmentRow;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Integratietests US-009: echte validator + mapper + sync-factory zonder mocks.
 */
class FhirValidationIntegrationTest {

  private FhirMessageValidator validator;
  private PatientFhirMapper patientMapper;
  private AppointmentFhirMapper appointmentMapper;
  private OpenmrsFhirResourceFactory resourceFactory;
  private FhirContext fhirContext;

  @BeforeEach
  void setUp() {
    validator = new FhirMessageValidator();
    patientMapper = new PatientFhirMapper(validator);
    appointmentMapper = new AppointmentFhirMapper(validator);
    resourceFactory = new OpenmrsFhirResourceFactory();
    fhirContext = FhirContext.forR5();
  }

  @Test
  void fixturePatientIsAcceptedByValidatorAndMapper() throws Exception {
    Patient patient = parsePatientFixture("fhir/patient-minimaal.json");

    assertTrue(validator.validatePatientResource(patient).isValid());
    Optional<PatientPollDto> mapped = patientMapper.mapPatient(patient);
    assertTrue(mapped.isPresent());
    assertEquals("+31612345678", mapped.get().phone());
  }

  @Test
  void fixtureAppointmentIsAcceptedByValidatorAndMapper() throws Exception {
    Appointment appointment = parseAppointmentFixture("fhir/appointment-minimaal.json");

    assertTrue(validator.validateAppointmentResource(appointment).isValid());
    assertTrue(appointmentMapper.map(appointment).isPresent());
  }

  @Test
  void syncFactoryPatientCompliesWithValidationRules() {
    OpenmrsSchedulingSyncProperties properties = new OpenmrsSchedulingSyncProperties();
    properties.setFallbackPhone("+31600000000");

    Patient patient = resourceFactory.buildPatient(sampleRow(null), properties);

    assertTrue(validator.validatePatientResource(patient).isValid());
    assertTrue(patientMapper.mapPatient(patient).isPresent());
  }

  @Test
  void syncFactoryWithoutPhoneFailsExport() {
    OpenmrsSchedulingSyncProperties properties = new OpenmrsSchedulingSyncProperties();
    properties.setFallbackPhone("");

    assertThrows(IllegalStateException.class, () -> resourceFactory.buildPatient(sampleRow(null), properties));
  }

  @Test
  void emptyBundleIsRejected() {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen entries"));
  }

  @Test
  void bundleWithoutPatientOrAppointmentIsRejected() {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.addEntry().setResource(new org.hl7.fhir.r5.model.OperationOutcome());

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen Patient of Appointment"));
  }

  @Test
  void validTransactionBundlePassesValidation() {
    Bundle bundle = new Bundle();
    bundle.setId("bundle-001");
    bundle.setType(Bundle.BundleType.TRANSACTION);
    bundle.addEntry().setResource(validPatient("pat-001"));
    Appointment appointment = new Appointment();
    appointment.setId("apt-001");
    appointment.setStart(new Date());
    appointment.setSubject(new Reference("Patient/pat-001"));
    bundle.addEntry().setResource(appointment);

    assertTrue(validator.validate(bundle).isValid());
  }

  private static Patient validPatient(String id) {
    Patient patient = new Patient();
    patient.setId(id);
    patient.addName().setFamily("Test").addGiven("Jan");
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    patient.addTelecom()
        .setSystem(ContactPoint.ContactPointSystem.PHONE)
        .setValue("+31612345678");
    return patient;
  }

  private static OpenmrsSchedulingAppointmentRow sampleRow(String phone) {
    return new OpenmrsSchedulingAppointmentRow(
        1,
        "appt-uuid",
        "SCHEDULED",
        false,
        null,
        3,
        "uuid-1",
        "Jan",
        "Jansen",
        LocalDateTime.of(2026, 5, 20, 8, 0),
        LocalDateTime.of(2026, 5, 20, 9, 0),
        "Consult",
        "loc-uuid",
        "Locatie",
        null,
        phone);
  }

  private Patient parsePatientFixture(String classpathPath) throws Exception {
    return (Patient) parseFixture(classpathPath);
  }

  private Appointment parseAppointmentFixture(String classpathPath) throws Exception {
    return (Appointment) parseFixture(classpathPath);
  }

  private org.hl7.fhir.instance.model.api.IBaseResource parseFixture(String classpathPath) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathPath)) {
      assertNotNull(in);
      String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return fhirContext.newJsonParser().parseResource(json);
    }
  }
}
