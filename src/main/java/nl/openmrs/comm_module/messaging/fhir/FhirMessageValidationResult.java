package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Bundle;

/**
 * Resultaat van FHIR-berichtvalidatie.
 */
public class FhirMessageValidationResult {

  private final boolean valid;
  private final String errorMessage;

  private FhirMessageValidationResult(boolean valid, String errorMessage) {
    this.valid = valid;
    this.errorMessage = errorMessage;
  }

  public static FhirMessageValidationResult valid() {
    return new FhirMessageValidationResult(true, null);
  }

  public static FhirMessageValidationResult invalid(String errorMessage) {
    return new FhirMessageValidationResult(false, errorMessage);
  }

  public boolean isValid() {
    return valid;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
