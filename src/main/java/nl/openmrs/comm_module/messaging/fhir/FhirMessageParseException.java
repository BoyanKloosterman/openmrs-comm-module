package nl.openmrs.comm_module.messaging.fhir;

/**
 * Fout bij het parsen of deserialiseren van een inkomend FHIR-bericht (US-010).
 */
public class FhirMessageParseException extends RuntimeException {

  public FhirMessageParseException(String message) {
    super(message);
  }

  public FhirMessageParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
