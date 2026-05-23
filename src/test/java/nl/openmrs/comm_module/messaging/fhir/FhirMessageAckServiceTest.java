package nl.openmrs.comm_module.messaging.fhir;

import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests voor FhirMessageAckService (US-010).
 */
class FhirMessageAckServiceTest {

  private FhirMessageAckService ackService;

  @BeforeEach
  void setUp() {
    ackService = new FhirMessageAckService();
  }

  // ============ ACK generation tests ============

  @Test
  void generateAck_validMessageId_shouldReturnOperationOutcomeWithSuccess() {
    String messageId = "msg-001";

    OperationOutcome ack = ackService.generateAck(messageId);

    assertNotNull(ack);
    assertFalse(ack.hasIssue() && ack.getIssue().isEmpty());
    assertTrue(ack.getId().contains(messageId));
    assertEquals(
        OperationOutcome.IssueSeverity.INFORMATION,
        ack.getIssueFirstRep().getSeverity());
    assertEquals(OperationOutcome.IssueType.INFORMATIONAL, ack.getIssueFirstRep().getCode());
    assertTrue(
        ack.getIssueFirstRep()
            .getDiagnostics()
            .contains("Bericht succesvol ontvangen"));
  }

  @Test
  void generateAck_nullMessageId_shouldReturnAckWithUnknown() {
    OperationOutcome ack = ackService.generateAck(null);

    assertNotNull(ack);
    assertTrue(ack.getId().contains("unknown"));
    assertTrue(
        ack.getIssueFirstRep()
            .getDiagnostics()
            .contains("succesvol ontvangen"));
  }

  @Test
  void generateAck_shouldIncludeMessageIdInDiagnostics() {
    String messageId = "msg-correlation-123";

    OperationOutcome ack = ackService.generateAck(messageId);

    assertTrue(ack.getIssueFirstRep().getDiagnostics().contains(messageId));
  }

  // ============ NACK generation tests (3-parameter version) ============

  @Test
  void generateNack_validInputWithErrorSeverity_shouldReturnOperationOutcomeWithError() {
    String messageId = "msg-002";
    String errorMessage = "Patient telecom is missing";
    OperationOutcome.IssueSeverity severity = OperationOutcome.IssueSeverity.ERROR;

    OperationOutcome nack = ackService.generateNack(messageId, errorMessage, severity);

    assertNotNull(nack);
    assertTrue(nack.getId().contains(messageId));
    assertEquals(severity, nack.getIssueFirstRep().getSeverity());
    assertEquals(OperationOutcome.IssueType.INVALID, nack.getIssueFirstRep().getCode());
    assertTrue(
        nack.getIssueFirstRep()
            .getDiagnostics()
            .contains("verwerking mislukt"));
    assertTrue(
        nack.getIssueFirstRep()
            .getDiagnostics()
            .contains(errorMessage));
  }

  @Test
  void generateNack_fatalSeverity_shouldReturnOperationOutcomeWithFatalSeverity() {
    String messageId = "msg-003";
    String errorMessage = "Bundle is null";
    OperationOutcome.IssueSeverity severity = OperationOutcome.IssueSeverity.FATAL;

    OperationOutcome nack = ackService.generateNack(messageId, errorMessage, severity);

    assertEquals(OperationOutcome.IssueSeverity.FATAL, nack.getIssueFirstRep().getSeverity());
  }

  @Test
  void generateNack_warningSeverity_shouldReturnOperationOutcomeWithWarningSeverity() {
    String messageId = "msg-004";
    String errorMessage = "Appointment start time is in the past";
    OperationOutcome.IssueSeverity severity = OperationOutcome.IssueSeverity.WARNING;

    OperationOutcome nack = ackService.generateNack(messageId, errorMessage, severity);

    assertEquals(OperationOutcome.IssueSeverity.WARNING, nack.getIssueFirstRep().getSeverity());
  }

  @Test
  void generateNack_nullSeverity_shouldDefaultToError() {
    String messageId = "msg-005";
    String errorMessage = "Validation failed";

    OperationOutcome nack = ackService.generateNack(messageId, errorMessage, null);

    assertEquals(OperationOutcome.IssueSeverity.ERROR, nack.getIssueFirstRep().getSeverity());
  }

  @Test
  void generateNack_shouldIncludeMessageIdInDiagnostics() {
    String messageId = "msg-error-456";
    String errorMessage = "Invalid format";

    OperationOutcome nack = ackService.generateNack(messageId, errorMessage);

    assertTrue(nack.getIssueFirstRep().getDiagnostics().contains(messageId));
    assertTrue(nack.getIssueFirstRep().getDiagnostics().contains(errorMessage));
  }

  // ============ NACK generation tests (2-parameter version) ============

  @Test
  void generateNack_twoParameters_shouldReturnErrorSeverityByDefault() {
    String messageId = "msg-006";
    String errorMessage = "Resource not found";

    OperationOutcome nack = ackService.generateNack(messageId, errorMessage);

    assertEquals(OperationOutcome.IssueSeverity.ERROR, nack.getIssueFirstRep().getSeverity());
    assertTrue(nack.getIssueFirstRep().getDiagnostics().contains(errorMessage));
  }

  @Test
  void generateNack_nullMessageIdTwoParameters_shouldReturnNackWithUnknown() {
    OperationOutcome nack = ackService.generateNack(null, "Some error");

    assertTrue(nack.getId().contains("unknown"));
  }

  // ============ Message ID extraction tests ============

  @Test
  void extractMessageId_bundleWithId_shouldReturnBundleId() {
    Bundle bundle = new Bundle();
    bundle.setId("bundle-123");

    String messageId = ackService.extractMessageId(bundle);

    assertEquals("bundle-123", messageId);
  }

  @Test
  void extractMessageId_bundleWithIdentifier_shouldReturnIdentifierValue() {
    Bundle bundle = new Bundle();
    Identifier identifier = new Identifier();
    identifier.setValue("msg-identifier-789");
    bundle.setIdentifier(identifier);

    String messageId = ackService.extractMessageId(bundle);

    assertEquals("msg-identifier-789", messageId);
  }

  @Test
  void extractMessageId_bundleWithoutIdAndIdentifier_shouldReturnUnknown() {
    Bundle bundle = new Bundle();

    String messageId = ackService.extractMessageId(bundle);

    assertEquals("unknown", messageId);
  }

  @Test
  void extractMessageId_nullBundle_shouldReturnUnknown() {
    String messageId = ackService.extractMessageId(null);

    assertEquals("unknown", messageId);
  }

  @Test
  void extractMessageId_bundleIdTakesPrecedenceOverIdentifier() {
    Bundle bundle = new Bundle();
    bundle.setId("bundle-priority");
    Identifier identifier = new Identifier();
    identifier.setValue("identifier-fallback");
    bundle.setIdentifier(identifier);

    String messageId = ackService.extractMessageId(bundle);

    assertEquals("bundle-priority", messageId);
  }

  // ============ Integration tests ============

  @Test
  void ackAndNackConsistency_shouldHaveSameMessageIdFormat() {
    String messageId = "msg-integration-001";

    OperationOutcome ack = ackService.generateAck(messageId);
    OperationOutcome nack = ackService.generateNack(messageId, "Test error");

    assertTrue(ack.getId().contains(messageId));
    assertTrue(nack.getId().contains(messageId));
  }

  @Test
  void fullWorkflow_receiveValidBundle_shouldGenerateAck() {
    Bundle bundle = new Bundle();
    bundle.setId("workflow-bundle-001");

    // Extract message ID from bundle
    String messageId = ackService.extractMessageId(bundle);

    // Generate ACK
    OperationOutcome ack = ackService.generateAck(messageId);

    assertEquals("workflow-bundle-001", messageId);
    assertTrue(ack.getId().contains(messageId));
    assertEquals(
        OperationOutcome.IssueSeverity.INFORMATION,
        ack.getIssueFirstRep().getSeverity());
  }

  @Test
  void fullWorkflow_receiveInvalidBundle_shouldGenerateNack() {
    Bundle bundle = new Bundle();
    bundle.setId("workflow-bundle-002");

    // Extract message ID
    String messageId = ackService.extractMessageId(bundle);

    // Generate NACK
    String errorMsg = "Bundle contains no entries";
    OperationOutcome nack = ackService.generateNack(messageId, errorMsg);

    assertEquals("workflow-bundle-002", messageId);
    assertTrue(nack.getId().contains(messageId));
    assertEquals(OperationOutcome.IssueSeverity.ERROR, nack.getIssueFirstRep().getSeverity());
    assertTrue(nack.getIssueFirstRep().getDiagnostics().contains(errorMsg));
  }
}
