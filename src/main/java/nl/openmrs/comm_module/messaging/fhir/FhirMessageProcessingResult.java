package nl.openmrs.comm_module.messaging.fhir;

/**
 * Resultaat van de verwerking van een gevalideerd FHIR-bericht (US-010).
 */
public class FhirMessageProcessingResult {

  private final boolean successful;
  private final String errorMessage;

  private FhirMessageProcessingResult(boolean successful, String errorMessage) {
    this.successful = successful;
    this.errorMessage = errorMessage;
  }

  public static FhirMessageProcessingResult success() {
    return new FhirMessageProcessingResult(true, null);
  }

  public static FhirMessageProcessingResult failure(String errorMessage) {
    return new FhirMessageProcessingResult(false, errorMessage);
  }

  public boolean isSuccessful() {
    return successful;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
