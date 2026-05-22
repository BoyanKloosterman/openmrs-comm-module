package nl.openmrs.comm_module.messaging.fhir;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import java.util.Date;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests voor FhirMessageValidator (US-009 en US-010).
 */
class FhirMessageValidatorTest {

  private FhirMessageValidator validator;
  private FhirContext fhirContext;

  @BeforeEach
  void setUp() {
    validator = new FhirMessageValidator();
    fhirContext = FhirContext.forR5();
  }

  // ============ Bundle-level validation tests ============

  @Test
  void validate_nullBundle_shouldReturnInvalid() {
    FhirMessageValidationResult result = validator.validate(null);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("null"));
  }

  @Test
  void validate_emptyBundle_shouldReturnInvalid() {
    Bundle bundle = new Bundle();

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen entries"));
  }

  // ============ Patient validation tests ============

  @Test
  void validate_patientWithoutId_shouldReturnInvalid() {
    Patient patient = new Patient();
    patient.setId(""); // lege id
    patient.addName(createValidHumanName());
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    patient.addTelecom(createValidContactPoint());

    Bundle bundle = createBundleWithEntry(patient);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen id"));
  }

  @Test
  void validate_patientWithoutName_shouldReturnInvalid() {
    Patient patient = new Patient();
    patient.setId("pat-001");
    patient.setGender(Enumerations.AdministrativeGender.FEMALE);
    patient.addTelecom(createValidContactPoint());

    Bundle bundle = createBundleWithEntry(patient);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen name"));
  }

  @Test
  void validate_patientWithoutGenderAndBirthDate_shouldReturnInvalid() {
    Patient patient = new Patient();
    patient.setId("pat-002");
    patient.addName(createValidHumanName());
    patient.addTelecom(createValidContactPoint());
    // geen gender, geen birthDate

    Bundle bundle = createBundleWithEntry(patient);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen gender") && result.getErrorMessage().contains("birthDate"));
  }

  @Test
  void validate_patientWithoutTelecom_shouldReturnInvalid() {
    Patient patient = new Patient();
    patient.setId("pat-003");
    patient.addName(createValidHumanName());
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    // geen telecom

    Bundle bundle = createBundleWithEntry(patient);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen telecom"));
  }

  @Test
  void validate_validPatient_shouldReturnValid() {
    Patient patient = createValidPatient("pat-001");

    Bundle bundle = createBundleWithEntry(patient);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertTrue(result.isValid());
  }

  // ============ Appointment validation tests ============

  @Test
  void validate_appointmentWithoutId_shouldReturnInvalid() {
    Appointment appointment = new Appointment();
    // geen id
    appointment.setStart(new Date());
    appointment.setSubject(new Reference("Patient/pat-001"));

    Bundle bundle = createBundleWithEntry(appointment);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen id"));
  }

  @Test
  void validate_appointmentWithoutStart_shouldReturnInvalid() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-001");
    // geen start
    appointment.setSubject(new Reference("Patient/pat-001"));

    Bundle bundle = createBundleWithEntry(appointment);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen start"));
  }

  @Test
  void validate_appointmentWithoutPatientReference_shouldReturnInvalid() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-002");
    appointment.setStart(new Date());
    // geen subject, geen participant met Patient-referentie

    Bundle bundle = createBundleWithEntry(appointment);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen patient-referentie"));
  }

  @Test
  void validate_validAppointmentWithSubject_shouldReturnValid() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-003");
    appointment.setStart(new Date());
    appointment.setSubject(new Reference("Patient/pat-001"));

    Bundle bundle = createBundleWithEntry(appointment);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertTrue(result.isValid());
  }

  @Test
  void validate_validAppointmentWithParticipant_shouldReturnValid() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-004");
    appointment.setStart(new Date());
    Appointment.AppointmentParticipantComponent participant = new Appointment.AppointmentParticipantComponent();
    participant.setActor(new Reference("Patient/pat-001"));
    appointment.addParticipant(participant);

    Bundle bundle = createBundleWithEntry(appointment);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertTrue(result.isValid());
  }

  // ============ Combined/integration tests ============

  @Test
  void validate_bundleWithValidPatientAndAppointment_shouldReturnValid() {
    Bundle bundle = new Bundle();
    bundle.addEntry().setResource(createValidPatient("pat-001"));

    Appointment appointment = new Appointment();
    appointment.setId("apt-001");
    appointment.setStart(new Date());
    appointment.setSubject(new Reference("Patient/pat-001"));
    bundle.addEntry().setResource(appointment);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertTrue(result.isValid());
  }

  @Test
  void validate_bundleWithInvalidPatientThenValidAppointment_shouldReturnInvalid() {
    Bundle bundle = new Bundle();

    // Invalid patient (no telecom)
    Patient patient = new Patient();
    patient.setId("pat-002");
    patient.addName(createValidHumanName());
    patient.setGender(Enumerations.AdministrativeGender.FEMALE);
    bundle.addEntry().setResource(patient);

    // Valid appointment (but validator will fail on patient first)
    Appointment appointment = new Appointment();
    appointment.setId("apt-002");
    appointment.setStart(new Date());
    appointment.setSubject(new Reference("Patient/pat-002"));
    bundle.addEntry().setResource(appointment);

    FhirMessageValidationResult result = validator.validate(bundle);

    assertFalse(result.isValid());
  }

  // ============ Single resource validation tests (US-009 polling integration)
  // ============

  @Test
  void validatePatientResource_nullPatient_shouldReturnInvalid() {
    FhirMessageValidationResult result = validator.validatePatientResource(null);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("null"));
  }

  @Test
  void validatePatientResource_validPatient_shouldReturnValid() {
    Patient patient = createValidPatient("pat-001");

    FhirMessageValidationResult result = validator.validatePatientResource(patient);

    assertTrue(result.isValid());
  }

  @Test
  void validatePatientResource_invalidPatient_shouldReturnInvalid() {
    Patient patient = new Patient();
    patient.setId("pat-invalid");
    patient.addName(createValidHumanName());
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    // geen telecom

    FhirMessageValidationResult result = validator.validatePatientResource(patient);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen telecom"));
  }

  @Test
  void validateAppointmentResource_nullAppointment_shouldReturnInvalid() {
    FhirMessageValidationResult result = validator.validateAppointmentResource(null);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("null"));
  }

  @Test
  void validateAppointmentResource_validAppointment_shouldReturnValid() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-001");
    appointment.setStart(new Date());
    appointment.setSubject(new Reference("Patient/pat-001"));

    FhirMessageValidationResult result = validator.validateAppointmentResource(appointment);

    assertTrue(result.isValid());
  }

  @Test
  void validateAppointmentResource_invalidAppointment_shouldReturnInvalid() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-invalid");
    appointment.setStart(new Date());
    // geen patient-referentie

    FhirMessageValidationResult result = validator.validateAppointmentResource(appointment);

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("geen patient-referentie"));
  }

  // ============ Helper methods ============

  private Bundle createBundleWithEntry(org.hl7.fhir.r5.model.Resource resource) {
    Bundle bundle = new Bundle();
    bundle.addEntry().setResource(resource);
    return bundle;
  }

  private Patient createValidPatient(String patientId) {
    Patient patient = new Patient();
    patient.setId(patientId);
    patient.addName(createValidHumanName());
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    patient.addTelecom(createValidContactPoint());
    return patient;
  }

  private HumanName createValidHumanName() {
    HumanName name = new HumanName();
    name.setFamily("Doe");
    name.addGiven("John");
    return name;
  }

  private ContactPoint createValidContactPoint() {
    ContactPoint contact = new ContactPoint();
    contact.setSystem(ContactPoint.ContactPointSystem.PHONE);
    contact.setValue("+31612345678");
    return contact;
  }
}
