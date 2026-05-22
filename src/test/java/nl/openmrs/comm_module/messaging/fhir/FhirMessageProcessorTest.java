package nl.openmrs.comm_module.messaging.fhir;

import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FhirMessageProcessorTest {

  private FhirMessageProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new FhirMessageProcessor();
  }

  @Test
  void process_bundleWithEntries_returnsSuccess() {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);
    bundle.addEntry().setResource(new Patient());

    FhirMessageProcessingResult result = processor.process(bundle);

    assertTrue(result.isSuccessful());
  }

  @Test
  void process_emptyBundle_returnsFailure() {
    Bundle bundle = new Bundle();

    FhirMessageProcessingResult result = processor.process(bundle);

    assertFalse(result.isSuccessful());
    assertTrue(result.getErrorMessage().contains("geen entries"));
  }

  @Test
  void process_nullBundle_returnsFailure() {
    FhirMessageProcessingResult result = processor.process(null);

    assertFalse(result.isSuccessful());
  }
}
