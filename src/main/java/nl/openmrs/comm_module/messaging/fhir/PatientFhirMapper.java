package nl.openmrs.comm_module.messaging.fhir;

import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapt FHIR R5 Patient naar poll-DTO.
 *
 * <p>
 * Integratie US-009: Valideert Patient-resources op verplichte velden
 * voordat ze worden gemapped (polling-laag validatie).
 */
@Component
public class PatientFhirMapper {

  private static final Logger log = LoggerFactory.getLogger(PatientFhirMapper.class);

  private final FhirMessageValidator validator;

  public PatientFhirMapper(FhirMessageValidator validator) {
    this.validator = validator;
  }

  /** Mapt FHIR R5 Patient naar poll-DTO; leeg zonder bruikbare id. */
  public Optional<PatientPollDto> mapPatient(Patient patient) {
    if (patient == null || !patient.hasId()) {
      log.debug("Patient is null of bevat geen id");
      return Optional.empty();
    }

    // US-009: Valideer het Patient-resource voordat het wordt gemapped
    FhirMessageValidationResult validationResult = validator.validatePatientResource(patient);
    if (!validationResult.isValid()) {
      // Ongeldige resource wordt geweigerd en gelogd
      log.warn("Patient {} validatie mislukt: {}", patient.getId(), validationResult.getErrorMessage());
      return Optional.empty();
    }

    String id = patient.getIdElement().getIdPart();
    String displayName = resolveDisplayName(patient);
    String phone = firstPhone(patient);
    return Optional.of(new PatientPollDto(id, displayName, phone));
  }

  private static String resolveDisplayName(Patient patient) {
    if (!patient.hasName()) {
      return null;
    }
    HumanName n = patient.getNameFirstRep();
    if (n.hasText()) {
      return n.getText();
    }
    if (n.hasFamily()) {
      String given = n.getGiven().stream()
          .map(StringType::getValue)
          .filter(s -> s != null && !s.isBlank())
          .collect(Collectors.joining(" "));
      String family = n.getFamily();
      if (!given.isEmpty()) {
        return given + " " + family;
      }
      return family;
    }
    return null;
  }

  /** Eerste telefoon- of sms-waarde voor notificaties. */
  private static String firstPhone(Patient patient) {
    for (ContactPoint cp : patient.getTelecom()) {
      if (!cp.hasValue()) {
        continue;
      }
      ContactPoint.ContactPointSystem sys = cp.getSystem();
      if (sys == ContactPoint.ContactPointSystem.PHONE
          || sys == ContactPoint.ContactPointSystem.SMS
          || sys == ContactPoint.ContactPointSystem.OTHER) {
        return cp.getValue();
      }
    }
    return null;
  }
}
