package nl.openmrs.comm_module.messaging.fhir;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Integratietests voor mappers met echte validators (US-009).
 * 
 * Deze tests gebruiken geen mocks - ze testen de volledige integratie van
 * validators en mappers om ervoor te zorgen dat ongeldige FHIR-resources
 * correct worden verworpen en valide resources correct worden gemapped.
 */
class FhirMapperValidatorIntegrationTest {

  private AppointmentFhirMapper appointmentMapper;
  private PatientFhirMapper patientMapper;
  private FhirMessageValidator validator;
  private FhirContext fhirContext;

  @BeforeEach
  void setUp() {
    fhirContext = FhirContext.forR5();
    validator = new FhirMessageValidator();
    appointmentMapper = new AppointmentFhirMapper(validator);
    patientMapper = new PatientFhirMapper(validator);
  }

  // ============ Patient Integration Tests ============

  @Test
  void patientWithValidDataIsMapped() {
    Patient patient = createValidPatient("pat-001", "Jan Jansen");

    Optional<PatientPollDto> result = patientMapper.mapPatient(patient);

    assertTrue(result.isPresent());
    PatientPollDto dto = result.get();
    assertEquals("pat-001", dto.patientId());
    assertEquals("Jan Jansen", dto.displayName());
  }

  @Test
  void patientWithInvalidIdIsNotMapped() {
    Patient patient = createPatientWithInvalidId("pat-001@invalid", "Jan Jansen");

    Optional<PatientPollDto> result = patientMapper.mapPatient(patient);

    assertTrue(result.isEmpty(), "Patient with invalid ID should not be mapped");
  }

  @Test
  void patientWithoutTelecomIsNotMapped() {
    Patient patient = new Patient();
    patient.setId("pat-002");
    patient.addName().setFamily("Jansen").addGiven("Jan");
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    // geen telecom

    Optional<PatientPollDto> result = patientMapper.mapPatient(patient);

    assertTrue(result.isEmpty(), "Patient without telecom should not be mapped");
  }

  @Test
  void patientWithEmptyTelecomIsNotMapped() {
    Patient patient = new Patient();
    patient.setId("pat-003");
    patient.addName().setFamily("Jansen").addGiven("Jan");
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    patient.addTelecom(); // telecom zonder value

    Optional<PatientPollDto> result = patientMapper.mapPatient(patient);

    assertTrue(result.isEmpty(), "Patient with empty telecom should not be mapped");
  }

  @Test
  void patientWithValidBirthdateAndNoGenderIsMapped() {
    Patient patient = new Patient();
    patient.setId("pat-004");
    patient.addName().setFamily("Jansen").addGiven("Jan");
    patient.setBirthDate(new Date());
    patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+31612345678");

    Optional<PatientPollDto> result = patientMapper.mapPatient(patient);

    assertTrue(result.isPresent(), "Patient with valid birthDate should be mapped");
  }

  // ============ Appointment Integration Tests ============

  @Test
  void appointmentWithValidDataIsMapped() {
    Appointment appointment = createValidAppointment("apt-001", "pat-001");

    Optional<AppointmentPollDto> result = appointmentMapper.map(appointment);

    assertTrue(result.isPresent());
    AppointmentPollDto dto = result.get();
    assertEquals("apt-001", dto.appointmentId());
    assertEquals("pat-001", dto.patientId());
  }

  @Test
  void appointmentWithInvalidIdIsNotMapped() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-001@invalid"); // invalid format
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(Date.from(Instant.now().plusSeconds(3600)));
    appointment.setSubject(new Reference("Patient/pat-001"));

    Optional<AppointmentPollDto> result = appointmentMapper.map(appointment);

    assertTrue(result.isEmpty(), "Appointment with invalid ID should not be mapped");
  }

  @Test
  void appointmentWithoutStartIsNotMapped() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-002");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    // no start
    appointment.setSubject(new Reference("Patient/pat-001"));

    Optional<AppointmentPollDto> result = appointmentMapper.map(appointment);

    assertTrue(result.isEmpty(), "Appointment without start should not be mapped");
  }

  @Test
  void appointmentWithInvalidDateFormatIsNotMapped() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-003");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(null); // no start
    appointment.setSubject(new Reference("Patient/pat-001"));

    Optional<AppointmentPollDto> result = appointmentMapper.map(appointment);

    assertTrue(result.isEmpty(), "Appointment with null start should not be mapped");
  }

  @Test
  void appointmentWithInvalidPatientReferenceFormatIsNotMapped() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-004");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(Date.from(Instant.now().plusSeconds(3600)));
    appointment.setSubject(new Reference("Patient/pat-001@invalid")); // invalid patient ID

    Optional<AppointmentPollDto> result = appointmentMapper.map(appointment);

    assertTrue(result.isEmpty(), "Appointment with invalid patient reference should not be mapped");
  }

  @Test
  void appointmentWithValidParticipantReferenceIsMapped() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-005");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(Date.from(Instant.now().plusSeconds(3600)));
    appointment.addParticipant().setActor(new Reference("Patient/pat-001"));

    Optional<AppointmentPollDto> result = appointmentMapper.map(appointment);

    assertTrue(result.isPresent(), "Appointment with valid participant reference should be mapped");
  }

  @Test
  void appointmentWithoutPatientReferenceIsNotMapped() {
    Appointment appointment = new Appointment();
    appointment.setId("apt-006");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(Date.from(Instant.now().plusSeconds(3600)));
    // no subject, no participants

    Optional<AppointmentPollDto> result = appointmentMapper.map(appointment);

    assertTrue(result.isEmpty(), "Appointment without patient reference should not be mapped");
  }

  // ============ Helper methods ============

  private Patient createValidPatient(String id, String name) {
    Patient patient = new Patient();
    patient.setId(id);
    // Parse name: "Jan Jansen" -> given="Jan", family="Jansen"
    String[] parts = name.split(" ");
    if (parts.length == 2) {
      patient.addName().setFamily(parts[1]).addGiven(parts[0]);
    } else {
      patient.addName().setFamily(name);
    }
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    patient.addTelecom()
        .setSystem(ContactPoint.ContactPointSystem.PHONE)
        .setValue("+31612345678");
    return patient;
  }

  private Patient createPatientWithInvalidId(String id, String name) {
    Patient patient = new Patient();
    patient.setId(id); // invalid format with @
    patient.addName().setFamily(name).addGiven("Jan");
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    patient.addTelecom()
        .setSystem(ContactPoint.ContactPointSystem.PHONE)
        .setValue("+31612345678");
    return patient;
  }

  private Appointment createValidAppointment(String id, String patientId) {
    Appointment appointment = new Appointment();
    appointment.setId(id);
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(Date.from(Instant.now().plusSeconds(3600)));
    appointment.setSubject(new Reference("Patient/" + patientId));
    return appointment;
  }
}
