package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Zet parse- en deserialisatiefouten om naar NACK (400) i.p.v. HTTP 500 (US-010).
 */
@RestControllerAdvice(assignableTypes = FhirMessageController.class)
public class FhirMessageExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(FhirMessageExceptionHandler.class);

  private final FhirMessageAckService ackService;

  public FhirMessageExceptionHandler(FhirMessageAckService ackService) {
    this.ackService = ackService;
  }

  @ExceptionHandler(FhirMessageParseException.class)
  public ResponseEntity<OperationOutcome> handleParseException(FhirMessageParseException ex) {
    log.warn("FHIR-bericht parsefout: {}", ex.getMessage());
    return nackResponse(ex.getMessage());
  }

  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      HttpMessageConversionException.class
  })
  public ResponseEntity<OperationOutcome> handleMessageConversion(Exception ex) {
    String errorMessage = resolveErrorMessage(ex);
    log.warn("FHIR-bericht deserialisatiefout: {}", errorMessage);
    return nackResponse(errorMessage);
  }

  private ResponseEntity<OperationOutcome> nackResponse(String errorMessage) {
    OperationOutcome nack = ackService.generateNack(
        "unknown",
        errorMessage,
        OperationOutcome.IssueSeverity.ERROR);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(nack);
  }

  private static String resolveErrorMessage(Throwable ex) {
    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
    if (cause instanceof FhirMessageParseException parseException) {
      return parseException.getMessage();
    }
    String message = cause.getMessage();
    return message != null && !message.isBlank()
        ? message
        : "Ongeldig FHIR-bericht: kon request body niet deserialiseren";
  }
}
