package nl.openmrs.comm_module.messaging.fhir;

import ca.uhn.fhir.context.FhirContext;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AppointmentFhirMapperTest {

  private AppointmentFhirMapper mapper;

  @Mock
  private FhirMessageValidator validator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mapper = new AppointmentFhirMapper(validator);

    // Default: alle valides resources zijn OK
    when(validator.validateAppointmentResource(any(Appointment.class)))
        .thenReturn(FhirMessageValidationResult.valid());
  }

  @Test
  void maptMinimaleAppointmentJson() throws Exception {
    Appointment appointment = parseFixture("fhir/appointment-minimaal.json");

    Optional<AppointmentPollDto> result = mapper.map(appointment);

    assertTrue(result.isPresent());
    AppointmentPollDto a = result.get();
    assertEquals("test-appointment-1", a.uuid());
    assertEquals("test-appointment-1", a.appointmentId());
    assertEquals("patient-42", a.patientId());
    assertEquals(Instant.parse("2026-05-14T10:15:30Z"), a.appointmentDatetime());
    assertNull(a.locationLabel());
    assertEquals("Follow-up visit", a.appointmentType());
    assertFalse(a.voided());
  }

  @Test
  void cancelledStatusGeldtAlsVoided() {
    Appointment appointment = appointmentMet(
        "u1",
        Appointment.AppointmentStatus.CANCELLED,
        Date.from(Instant.parse("2026-01-01T10:00:00Z")),
        patientRef("p1"),
        null);

    assertTrue(mapper.map(appointment).orElseThrow().voided());
  }

  @Test
  void noshowStatusGeldtAlsVoided() {
    Appointment appointment = appointmentMet(
        "u1b",
        Appointment.AppointmentStatus.NOSHOW,
        Date.from(Instant.parse("2026-01-01T10:00:00Z")),
        patientRef("p1"),
        null);

    assertTrue(mapper.map(appointment).orElseThrow().voided());
  }

  @Test
  void zonderPatientLeeg() {
    Appointment appointment = new Appointment();
    appointment.setId("u3");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(Date.from(Instant.parse("2026-01-01T10:00:00Z")));

    assertTrue(mapper.map(appointment).isEmpty());
  }

  @Test
  void zonderStartLeeg() {
    Appointment appointment = new Appointment();
    appointment.setId("u4");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setSubject(patientRef("p1"));

    assertTrue(mapper.map(appointment).isEmpty());
  }

  @Test
  void haaltPatientUitParticipant() {
    Appointment appointment = new Appointment();
    appointment.setId("u5");
    appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
    appointment.setStart(Date.from(Instant.parse("2026-05-14T10:15:30Z")));
    appointment.addParticipant().setActor(patientRef("patient-99"));

    assertEquals("patient-99", mapper.map(appointment).orElseThrow().patientId());
  }

  @Test
  void haaltLocatieUitParticipant() {
    Appointment appointment = appointmentMet(
        "u6",
        Appointment.AppointmentStatus.BOOKED,
        Date.from(Instant.parse("2026-01-01T10:00:00Z")),
        patientRef("p1"),
        new Reference("Location/loc-7"));

    assertEquals("loc-7", mapper.map(appointment).orElseThrow().locationLabel());
  }

  @Test
  void leestOpenmrsLocatieUitExtension() {
    Appointment appointment = appointmentMet(
        "u7",
        Appointment.AppointmentStatus.BOOKED,
        Date.from(Instant.parse("2026-01-01T10:00:00Z")),
        patientRef("p1"),
        null);
    OpenmrsFhirAppointmentMetadata.applyTo(appointment, "Poli A - Kamer 1", "Medicijnen meenemen");

    AppointmentPollDto dto = mapper.map(appointment).orElseThrow();
    assertEquals("Poli A - Kamer 1", dto.locationLabel());
    assertEquals("Medicijnen meenemen", dto.reason());
  }

  // US-009 Validation tests
  @Test
  void validatieFailureRetourneertLeegOptional() {
    Appointment appointment = appointmentMet(
        "u8",
        Appointment.AppointmentStatus.BOOKED,
        Date.from(Instant.parse("2026-01-01T10:00:00Z")),
        patientRef("p1"),
        null);

    when(validator.validateAppointmentResource(appointment))
        .thenReturn(FhirMessageValidationResult.invalid("Appointment bevat geen start"));

    Optional<AppointmentPollDto> result = mapper.map(appointment);
    assertTrue(result.isEmpty());
  }

  @Test
  void validatieSuccesResulteertInMapping() {
    Appointment appointment = appointmentMet(
        "u9",
        Appointment.AppointmentStatus.BOOKED,
        Date.from(Instant.parse("2026-01-01T10:00:00Z")),
        patientRef("p1"),
        null);

    when(validator.validateAppointmentResource(appointment))
        .thenReturn(FhirMessageValidationResult.valid());

    Optional<AppointmentPollDto> result = mapper.map(appointment);
    assertTrue(result.isPresent());
    assertEquals("p1", result.get().patientId());
  }

  private static Appointment appointmentMet(
      String id,
      Appointment.AppointmentStatus status,
      Date start,
      Reference patient,
      Reference location) {
    Appointment appointment = new Appointment();
    appointment.setId(id);
    appointment.setStatus(status);
    appointment.setStart(start);
    if (patient != null) {
      appointment.setSubject(patient);
    }
    if (location != null) {
      appointment.addParticipant().setActor(location);
    }
    return appointment;
  }

  private static Reference patientRef(String patientId) {
    return new Reference("Patient/" + patientId);
  }

  private static Appointment parseFixture(String classpathPath) throws Exception {
    FhirContext ctx = FhirContext.forR5();
    try (InputStream in = AppointmentFhirMapperTest.class.getClassLoader().getResourceAsStream(classpathPath)) {
      assertNotNull(in);
      String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return (Appointment) ctx.newJsonParser().parseResource(json);
    }
  }
}
