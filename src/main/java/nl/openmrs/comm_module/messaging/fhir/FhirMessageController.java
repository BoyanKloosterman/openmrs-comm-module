package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-controller voor ontvangst van FHIR-berichten en verzending van
 * ACK/NACK-responses (US-010).
 *
 * <p>
 * Endpoints:
 * - POST /api/fhir/messages — Ontvang een FHIR-bericht en verstuur ACK of NACK
 *
 * <p>
 * Acceptatiecriteria (US-010):
 * - Het systeem stuurt een ACK terug na succesvolle verwerking.
 * - Het systeem stuurt een NACK terug bij een fout of ongeldig bericht.
 * - De ACK bevat de juiste berichtidentificatie.
 */
@RestController
@RequestMapping("/api/fhir/messages")
public class FhirMessageController {

  private static final Logger log = LoggerFactory.getLogger(FhirMessageController.class);

  private final FhirMessageAckService ackService;
  private final FhirMessageValidator messageValidator;

  public FhirMessageController(
      FhirMessageAckService ackService,
      FhirMessageValidator messageValidator) {
    this.ackService = ackService;
    this.messageValidator = messageValidator;
  }

  /**
   * Ontvang een FHIR-bericht (Bundle) en verstuur ACK/NACK.
   *
   * <p>
   * Request body: FHIR Bundle (JSON) met entry's van Message, Appointment,
   * Patient, etc.
   * Response: FHIR OperationOutcome met ACK (success) of NACK (error)
   *
   * @param bundle De inkomende FHIR Bundle
   * @return ResponseEntity met OperationOutcome (ACK/NACK)
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationOutcome> receiveMessage(@RequestBody Bundle bundle) {
    log.info("FHIR-bericht ontvangen");

    // Valideer het bericht
    FhirMessageValidationResult validationResult = messageValidator.validate(bundle);

    if (!validationResult.isValid()) {
      // NACK bij validatiefout
      String messageId = ackService.extractMessageId(bundle);
      OperationOutcome nack = ackService.generateNack(
          messageId,
          validationResult.getErrorMessage(),
          OperationOutcome.IssueSeverity.ERROR);
      log.warn("FHIR-bericht validatie mislukt: {}", validationResult.getErrorMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(nack);
    }

    // ACK bij succesvolle verwerking
    String messageId = ackService.extractMessageId(bundle);
    OperationOutcome ack = ackService.generateAck(messageId);

    log.info("FHIR-bericht succesvol verwerkt en ACK verstuurd voor message ID: {}", messageId);
    return ResponseEntity.status(HttpStatus.OK).body(ack);
  }
}
