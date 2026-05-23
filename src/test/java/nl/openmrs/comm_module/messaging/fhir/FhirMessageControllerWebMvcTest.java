package nl.openmrs.comm_module.messaging.fhir;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.context.FhirContext;
import java.util.Date;
import nl.openmrs.comm_module.config.FhirR5HttpMessageConverter;
import nl.openmrs.comm_module.config.OpenmrsFhirConfig;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * WebMvc end-to-end tests voor ACK/NACK (US-010) met echte JSON en FHIR-converter.
 */
class FhirMessageControllerWebMvcTest {

  private MockMvc mockMvc;
  private FhirContext fhirContext;

  @BeforeEach
  void setUp() {
    fhirContext = new OpenmrsFhirConfig().fhirContextR5();
    FhirR5HttpMessageConverter fhirConverter = new FhirR5HttpMessageConverter(fhirContext);
    FhirMessageAckService ackService = new FhirMessageAckService();
    FhirMessageValidator validator = new FhirMessageValidator();
    FhirMessageProcessor processor = new FhirMessageProcessor();

    FhirMessageController controller =
        new FhirMessageController(ackService, validator, processor);

    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setMessageConverters(fhirConverter)
        .setControllerAdvice(new FhirMessageExceptionHandler(ackService))
        .build();
  }

  @Test
  void post_validBundle_returns200AckAsOperationOutcome() throws Exception {
    String body = fhirContext.newJsonParser().encodeResourceToString(createValidBundle());

    mockMvc.perform(post("/api/fhir/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(new MediaType("application", "fhir+json")))
        .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
        .andExpect(jsonPath("$.id").value("ack-msg-us010-001"))
        .andExpect(jsonPath("$.issue[0].severity").value("information"))
        .andExpect(jsonPath("$.issue[0].diagnostics", containsString("msg-us010-001")));
  }

  @Test
  void post_invalidBundle_returns400NackAsOperationOutcome() throws Exception {
    Bundle invalidBundle = new Bundle();
    invalidBundle.setType(Bundle.BundleType.TRANSACTION);
    String body = fhirContext.newJsonParser().encodeResourceToString(invalidBundle);

    mockMvc.perform(post("/api/fhir/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
        .andExpect(jsonPath("$.issue[0].severity").value("error"))
        .andExpect(jsonPath("$.issue[0].diagnostics", containsString("geen entries")));
  }

  @Test
  void post_malformedJson_returns400NackNot500() throws Exception {
    mockMvc.perform(post("/api/fhir/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content("{not-valid-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
        .andExpect(jsonPath("$.issue[0].severity").value("error"));
  }

  @Test
  void post_nonBundleResource_returns400Nack() throws Exception {
    Patient patient = new Patient();
    patient.setId("pat-only");
    String body = fhirContext.newJsonParser().encodeResourceToString(patient);

    mockMvc.perform(post("/api/fhir/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
        .andExpect(jsonPath("$.issue[0].diagnostics", containsString("Bundle")));
  }

  @Test
  void post_bundleWithInvalidPatient_returns400Nack() throws Exception {
    Bundle bundle = new Bundle();
    bundle.setId("msg-invalid-patient");
    bundle.setType(Bundle.BundleType.TRANSACTION);
    Patient patient = new Patient();
    patient.setId("pat-001@invalid");
    patient.addName().setFamily("Doe").addGiven("John");
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+31612345678");
    bundle.addEntry().setResource(patient);
    String body = fhirContext.newJsonParser().encodeResourceToString(bundle);

    mockMvc.perform(post("/api/fhir/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.id").value("nack-msg-invalid-patient"))
        .andExpect(jsonPath("$.issue[0].severity").value("error"));
  }

  private Bundle createValidBundle() {
    Bundle bundle = new Bundle();
    bundle.setId("msg-us010-001");
    bundle.setType(Bundle.BundleType.TRANSACTION);

    Patient patient = new Patient();
    patient.setId("pat-001");
    HumanName name = new HumanName();
    name.setFamily("Doe");
    name.addGiven("John");
    patient.addName(name);
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    ContactPoint contact = new ContactPoint();
    contact.setSystem(ContactPoint.ContactPointSystem.PHONE);
    contact.setValue("+31612345678");
    patient.addTelecom(contact);
    bundle.addEntry().setResource(patient);

    Appointment appointment = new Appointment();
    appointment.setId("apt-001");
    appointment.setStart(new Date());
    appointment.setSubject(new Reference("Patient/pat-001"));
    bundle.addEntry().setResource(appointment);

    return bundle;
  }
}
