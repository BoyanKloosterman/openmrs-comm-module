package nl.openmrs.comm_module.messaging.fhir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests voor FhirMessageController (US-010).
 */
class FhirMessageControllerTest {

  private FhirMessageController controller;
  private FhirMessageAckService ackService;
  private FhirMessageValidator messageValidator;
  private FhirMessageProcessor messageProcessor;

  @BeforeEach
  void setUp() {
    ackService = mock(FhirMessageAckService.class);
    messageValidator = mock(FhirMessageValidator.class);
    messageProcessor = mock(FhirMessageProcessor.class);

    controller = new FhirMessageController(ackService, messageValidator, messageProcessor);
  }

  // ============ Happy path tests ============

  @Test
  void receiveMessage_validBundle_shouldReturnAckWithOkStatus() {
    Bundle bundle = createValidBundle();
    String messageId = "msg-001";

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.valid());
    when(messageProcessor.process(any(Bundle.class)))
        .thenReturn(FhirMessageProcessingResult.success());
    when(ackService.extractMessageId(bundle)).thenReturn(messageId);

    OperationOutcome ack = new OperationOutcome();
    ack.setId("ack-msg-001");
    when(ackService.generateAck(messageId)).thenReturn(ack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundle);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(messageValidator, times(1)).validate(any(Bundle.class));
    verify(messageProcessor, times(1)).process(any(Bundle.class));
    verify(ackService, times(1)).generateAck(messageId);
    verify(ackService, never()).generateNack(any(), any(), any());
  }

  @Test
  void receiveMessage_validBundleResponseContainsMessageId() {
    Bundle bundle = createValidBundle();
    String messageId = "msg-response-001";

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.valid());
    when(messageProcessor.process(any(Bundle.class)))
        .thenReturn(FhirMessageProcessingResult.success());
    when(ackService.extractMessageId(bundle)).thenReturn(messageId);

    OperationOutcome ack = new OperationOutcome();
    ack.setId("ack-" + messageId);
    OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
    issue.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
    issue.setDiagnostics("Message ID: " + messageId);
    ack.addIssue(issue);

    when(ackService.generateAck(messageId)).thenReturn(ack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundle);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("ack-" + messageId, response.getBody().getId());
  }

  // ============ Error path tests (validation fails) ============

  @Test
  void receiveMessage_invalidBundle_shouldReturnNackWithBadRequestStatus() {
    Bundle bundle = new Bundle();
    String errorMsg = "Bundle contains no entries";

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.invalid(errorMsg));
    when(ackService.extractMessageId(any(Bundle.class))).thenReturn("msg-invalid");

    OperationOutcome nack = new OperationOutcome();
    nack.setId("nack-msg-invalid");
    OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
    issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
    issue.setDiagnostics(errorMsg);
    nack.addIssue(issue);

    when(ackService.generateNack("msg-invalid", errorMsg, OperationOutcome.IssueSeverity.ERROR))
        .thenReturn(nack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundle);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("nack-msg-invalid", response.getBody().getId());
    assertEquals(OperationOutcome.IssueSeverity.ERROR, response.getBody().getIssueFirstRep().getSeverity());

    verify(messageValidator, times(1)).validate(any(Bundle.class));
    verify(ackService, times(1))
        .generateNack("msg-invalid", errorMsg, OperationOutcome.IssueSeverity.ERROR);
    verify(messageProcessor, never()).process(any(Bundle.class));
  }

  @Test
  void receiveMessage_processingFails_shouldReturnNackWithoutCallingAck() {
    Bundle bundle = createValidBundle();

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.valid());
    when(messageProcessor.process(any(Bundle.class)))
        .thenReturn(FhirMessageProcessingResult.failure("Verwerking mislukt"));
    when(ackService.extractMessageId(bundle)).thenReturn("msg-001");

    OperationOutcome nack = new OperationOutcome();
    when(ackService.generateNack("msg-001", "Verwerking mislukt", OperationOutcome.IssueSeverity.ERROR))
        .thenReturn(nack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundle);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(messageProcessor, times(1)).process(any(Bundle.class));
    verify(ackService, never()).generateAck(any());
  }

  @Test
  void receiveMessage_patientValidationFails_shouldReturnNack() {
    Bundle bundle = new Bundle();
    String errorMsg = "Patient validation failed: missing telecom";

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.invalid(errorMsg));
    when(ackService.extractMessageId(any(Bundle.class))).thenReturn("msg-patient-001");

    OperationOutcome nack = new OperationOutcome();
    nack.setId("nack-msg-patient-001");
    when(ackService.generateNack(
        "msg-patient-001", errorMsg, OperationOutcome.IssueSeverity.ERROR))
        .thenReturn(nack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundle);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(ackService, times(1))
        .generateNack(
            "msg-patient-001", errorMsg, OperationOutcome.IssueSeverity.ERROR);
  }

  @Test
  void receiveMessage_appointmentValidationFails_shouldReturnNack() {
    Bundle bundle = new Bundle();
    String errorMsg = "Appointment validation failed: missing start time";

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.invalid(errorMsg));
    when(ackService.extractMessageId(any(Bundle.class))).thenReturn("msg-apt-001");

    OperationOutcome nack = new OperationOutcome();
    nack.setId("nack-msg-apt-001");
    when(ackService.generateNack(
        "msg-apt-001", errorMsg, OperationOutcome.IssueSeverity.ERROR))
        .thenReturn(nack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundle);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  // ============ Edge cases ============

  @Test
  void receiveMessage_emptyJsonBody_shouldFail() {
    Bundle bundle = new Bundle();

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.invalid("No entries"));
    when(ackService.extractMessageId(any(Bundle.class))).thenReturn("unknown");

    OperationOutcome nack = new OperationOutcome();
    when(ackService.generateNack(
        "unknown", "No entries", OperationOutcome.IssueSeverity.ERROR))
        .thenReturn(nack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundle);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void receiveMessage_bundleWithoutId_shouldExtractMessageIdAsUnknown() {
    Bundle bundleWithoutId = new Bundle();

    when(messageValidator.validate(any(Bundle.class)))
        .thenReturn(FhirMessageValidationResult.invalid("No entries"));
    when(ackService.extractMessageId(any(Bundle.class))).thenReturn("unknown");

    OperationOutcome nack = new OperationOutcome();
    nack.setId("nack-unknown");
    when(ackService.generateNack(
        "unknown", "No entries", OperationOutcome.IssueSeverity.ERROR))
        .thenReturn(nack);

    ResponseEntity<OperationOutcome> response = controller.receiveMessage(bundleWithoutId);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(ackService, times(1)).extractMessageId(any(Bundle.class));
  }

  // ============ Helper methods ============

  private Bundle createValidBundle() {
    Bundle bundle = new Bundle();
    bundle.setId("test-bundle-001");

    // Add valid patient
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

    // Add valid appointment
    Appointment apt = new Appointment();
    apt.setId("apt-001");
    apt.setStart(new Date());
    apt.setSubject(new Reference("Patient/pat-001"));
    bundle.addEntry().setResource(apt);

    return bundle;
  }

}
