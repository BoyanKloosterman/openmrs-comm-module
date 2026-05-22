package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service voor het genereren van ACK- en NACK-berichten bij ontvangst van
 * FHIR-berichten (US-010).
 *
 * <p>
 * Acceptatiecriteria:
 * - Het systeem stuurt een ACK terug na succesvolle verwerking.
 * - Het systeem stuurt een NACK terug bij een fout of ongeldig bericht.
 * - De ACK bevat de juiste berichtidentificatie.
 */
@Service
public class FhirMessageAckService {

  private static final Logger log = LoggerFactory.getLogger(FhirMessageAckService.class);

  /**
   * Genereert een ACK (OperationOutcome met SUCCESS) voor succesvolle
   * berichtverwerking.
   *
   * @param messageId De identificatie van het ontvangen bericht (van Bundle.id of
   *                  Message.id)
   * @return OperationOutcome met success-status en berichtidentificatie
   */
  public OperationOutcome generateAck(String messageId) {
    log.debug("ACK genereren voor bericht: {}", messageId);

    OperationOutcome outcome = new OperationOutcome();
    outcome.setId("ack-" + (messageId != null ? messageId : "unknown"));

    OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
    issue.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
    issue.setCode(OperationOutcome.IssueType.INFORMATIONAL);
    issue.setDiagnostics("Bericht succesvol ontvangen en verwerkt. Message ID: " + messageId);

    outcome.addIssue(issue);

    log.info("ACK gegenereerd voor bericht: {}", messageId);
    return outcome;
  }

  /**
   * Genereert een NACK (OperationOutcome met ERROR) voor foutieve
   * berichtverwerking.
   *
   * @param messageId     De identificatie van het ontvangen bericht
   * @param errorMessage  Beschrijving van de fout
   * @param issueSeverity Severity level (FATAL, ERROR, WARNING)
   * @return OperationOutcome met error-status en berichtidentificatie
   */
  public OperationOutcome generateNack(
      String messageId,
      String errorMessage,
      OperationOutcome.IssueSeverity issueSeverity) {
    log.warn("NACK genereren voor bericht {}: {}", messageId, errorMessage);

    OperationOutcome outcome = new OperationOutcome();
    outcome.setId("nack-" + (messageId != null ? messageId : "unknown"));

    OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
    issue.setSeverity(issueSeverity != null ? issueSeverity : OperationOutcome.IssueSeverity.ERROR);
    issue.setCode(OperationOutcome.IssueType.INVALID);
    issue.setDiagnostics(
        "Bericht verwerking mislukt. Message ID: "
            + messageId
            + ". Error: "
            + errorMessage);

    outcome.addIssue(issue);

    log.warn("NACK gegenereerd voor bericht: {} met severity: {}", messageId, issue.getSeverity());
    return outcome;
  }

  /**
   * Genereert een NACK voor ongeldig bericht (standaard ERROR severity).
   *
   * @param messageId    De identificatie van het bericht
   * @param errorMessage Beschrijving van het validatieprobleem
   * @return OperationOutcome met error-status
   */
  public OperationOutcome generateNack(String messageId, String errorMessage) {
    return generateNack(messageId, errorMessage, OperationOutcome.IssueSeverity.ERROR);
  }

  /**
   * Extraheert de berichtidentificatie uit een Bundle (voor tracking in
   * ACK/NACK).
   *
   * @param bundle De ontvangen FHIR Bundle
   * @return Message ID, of "unknown" indien niet gevonden
   */
  public String extractMessageId(Bundle bundle) {
    if (bundle == null) {
      return "unknown";
    }

    // Probeer eerst Bundle.id
    if (bundle.hasId()) {
      return bundle.getId();
    }

    // Fallback: identifier als aanwezig
    if (bundle.hasIdentifier()) {
      return bundle.getIdentifier().getValue();
    }

    return "unknown";
  }
}
