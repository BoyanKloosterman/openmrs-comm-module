package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Verwerkt gevalideerde FHIR-berichten na succesvolle validatie (US-010).
 */
@Service
public class FhirMessageProcessor {

  private static final Logger log = LoggerFactory.getLogger(FhirMessageProcessor.class);

  /**
   * Verwerkt een reeds gevalideerde FHIR Bundle.
   *
   * @param bundle de gevalideerde bundle
   * @return verwerkingsresultaat; bij fout wordt een NACK teruggestuurd
   */
  public FhirMessageProcessingResult process(Bundle bundle) {
    if (bundle == null) {
      return FhirMessageProcessingResult.failure("FHIR Bundle is null");
    }

    int entryCount = bundle.getEntry() != null ? bundle.getEntry().size() : 0;
    log.info("FHIR-bericht verwerken: {} entries", entryCount);

    if (entryCount == 0) {
      return FhirMessageProcessingResult.failure("FHIR Bundle bevat geen entries om te verwerken");
    }

    log.debug("FHIR-bericht verwerking succesvol afgerond");
    return FhirMessageProcessingResult.success();
  }
}
