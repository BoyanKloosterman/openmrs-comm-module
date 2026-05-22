package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validator voor inkomende FHIR-berichten (US-009 en US-010).
 *
 * <p>
 * Acceptatiecriteria (US-009):
 * - Het systeem controleert of een FHIR Patient-resource alle verplichte velden
 * bevat.
 * - Een ongeldige Patient-resource wordt geweigerd en de fout wordt gelogd.
 * - Een geldige resource wordt doorgegeven.
 * - De FHIR R5 Appointment-resource wordt gecontroleerd op id, start en
 * patient-referentie.
 */
@Service
public class FhirMessageValidator {

  private static final Logger log = LoggerFactory.getLogger(FhirMessageValidator.class);

  /**
   * Valideert een inkomende FHIR Bundle.
   *
   * <p>
   * Controleert:
   * - Bundle is niet null
   * - Bundle bevat entries
   * - Patient entries hebben verplichte velden (id, name, gender/birthDate,
   * telecom)
   * - Appointment entries hebben verplichte velden (id, start,
   * patient-referentie)
   *
   * @param bundle De FHIR Bundle om te valideren
   * @return FhirMessageValidationResult met validatiestatus en eventuele
   *         foutmelding
   */
  public FhirMessageValidationResult validate(Bundle bundle) {
    if (bundle == null) {
      log.warn("FHIR Bundle is null");
      return FhirMessageValidationResult.invalid("FHIR Bundle is null");
    }

    // Valideer Bundle structuur
    FhirMessageValidationResult structureValidation = validateBundleStructure(bundle);
    if (!structureValidation.isValid()) {
      return structureValidation;
    }

    if (!hasBundleEntries(bundle)) {
      log.warn("FHIR Bundle bevat geen entries");
      return FhirMessageValidationResult.invalid("FHIR Bundle bevat geen entries");
    }

    boolean hasSupportedResource = false;
    int entryIndex = 0;
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource() == null) {
        log.warn("Entry {} bevat geen resource", entryIndex);
        return FhirMessageValidationResult.invalid(
            "Entry " + entryIndex + " bevat geen resource");
      }

      if (entry.getResource() instanceof Patient patient) {
        hasSupportedResource = true;
        FhirMessageValidationResult patientValidation = validatePatient(patient, entryIndex);
        if (!patientValidation.isValid()) {
          return patientValidation;
        }
      } else if (entry.getResource() instanceof Appointment appointment) {
        hasSupportedResource = true;
        FhirMessageValidationResult appointmentValidation = validateAppointment(appointment, entryIndex);
        if (!appointmentValidation.isValid()) {
          return appointmentValidation;
        }
      }

      entryIndex++;
    }

    if (!hasSupportedResource) {
      String msg = "Bundle bevat geen Patient of Appointment resources";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    log.debug("FHIR Bundle validatie succesvol");
    return FhirMessageValidationResult.valid();
  }

  /**
   * Valideert de FHIR Bundle-structuur (US-009 uitbreiding).
   * 
   * <p>
   * Controleert:
   * - Bundle heeft geldige type
   * - Bundle ID format (indien aanwezig)
   */
  private FhirMessageValidationResult validateBundleStructure(Bundle bundle) {
    // Valideer Bundle type
    if (!bundle.hasType()) {
      String msg = "Bundle bevat geen type";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    // Valideer dat Bundle type geldig is
    try {
      Bundle.BundleType bundleType = bundle.getType();
      if (bundleType == null) {
        String msg = "Bundle type is ongeldig";
        log.warn(msg);
        return FhirMessageValidationResult.invalid(msg);
      }
    } catch (Exception e) {
      String msg = "Bundle type format is ongeldig: " + e.getMessage();
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (bundle.hasId()) {
      String bundleId = bundle.getIdElement().getIdPart();
      if (bundleId.isBlank() || !isValidFhirId(bundleId)) {
        String msg = "Bundle bevat ongeldig id format: " + bundleId;
        log.warn(msg);
        return FhirMessageValidationResult.invalid(msg);
      }
    }

    log.debug("Bundle structuur validatie succesvol");
    return FhirMessageValidationResult.valid();
  }

  /**
   * Valideert verplichte velden van een FHIR Patient-resource.
   *
   * <p>
   * Verplichte velden:
   * - id
   * - name (minimaal één name element met gegeven of familienaam)
   * - gender OF birthDate (minimaal één moet aanwezig zijn)
   * - telecom (minimaal één contactmogelijkheid, bijv. phone)
   *
   * @param patient    De Patient-resource om te valideren
   * @param entryIndex Index in de bundle voor foutmeldingen
   * @return FhirMessageValidationResult met validatiestatus
   */
  private FhirMessageValidationResult validatePatient(Patient patient, int entryIndex) {
    if (!patient.hasId() || patient.getIdElement().getIdPart().isBlank()) {
      String msg = "Patient (entry " + entryIndex + ") bevat geen id";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    String patientId = patient.getIdElement().getIdPart();

    // Syntax validatie: id format
    if (!isValidFhirId(patientId)) {
      String msg = "Patient " + patientId + " (entry " + entryIndex + ") bevat ongeldig id format";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!patient.hasName() || patient.getName().isEmpty()) {
      String msg = "Patient " + patientId + " (entry " + entryIndex + ") bevat geen name";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    boolean hasValidName = patient.getName().stream()
        .anyMatch(name -> name.hasGiven() || name.hasFamily());
    if (!hasValidName) {
      String msg = "Patient "
          + patientId
          + " (entry "
          + entryIndex
          + ") bevat geen gegeven of familienaam";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!patient.hasGender() && !patient.hasBirthDate()) {
      String msg = "Patient "
          + patientId
          + " (entry "
          + entryIndex
          + ") bevat geen gender en geen birthDate";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    // Syntax validatie: birthDate format (als aanwezig)
    if (patient.hasBirthDate() && patient.getBirthDate() == null) {
      String msg = "Patient " + patientId + " (entry " + entryIndex + ") bevat ongeldig birthDate format";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!hasPhoneTelecom(patient)) {
      String msg = "Patient "
          + patientId
          + " (entry "
          + entryIndex
          + ") bevat geen telefoon in telecom";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    log.debug("Patient {} validatie succesvol", patientId);
    return FhirMessageValidationResult.valid();
  }

  /**
   * Valideert verplichte velden van een FHIR Appointment-resource.
   *
   * <p>
   * Verplichte velden:
   * - id
   * - start (afspraaktijd)
   * - patient-referentie (subject of participant met Patient-referentie)
   *
   * @param appointment De Appointment-resource om te valideren
   * @param entryIndex  Index in de bundle voor foutmeldingen
   * @return FhirMessageValidationResult met validatiestatus
   */
  private FhirMessageValidationResult validateAppointment(Appointment appointment, int entryIndex) {
    if (!appointment.hasId() || appointment.getIdElement().getIdPart().isBlank()) {
      String msg = "Appointment (entry " + entryIndex + ") bevat geen id";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    String appointmentId = appointment.getIdElement().getIdPart();

    // Syntax validatie: id format
    if (!isValidFhirId(appointmentId)) {
      String msg = "Appointment " + appointmentId + " (entry " + entryIndex + ") bevat ongeldig id format";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!appointment.hasStart()) {
      String msg = "Appointment "
          + appointmentId
          + " (entry "
          + entryIndex
          + ") bevat geen start (afspraaktijd)";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    // Syntax validatie: start is valide datum/tijd
    if (appointment.getStart() == null) {
      String msg = "Appointment " + appointmentId + " (entry " + entryIndex + ") bevat ongeldig start format";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    // Syntax validatie: start is niet in het verleden (of minimaal vandaag)
    if (appointment.getStart().before(new java.util.Date())) {
      log.debug("Appointment " + appointmentId + " (entry " + entryIndex + ") ligt in het verleden");
    }

    // Check voor patient-referentie via subject of participant
    boolean hasPatientReference = (appointment.hasSubject() && appointment.getSubject().hasReference())
        || (appointment.hasParticipant()
            && appointment.getParticipant().stream()
                .anyMatch(p -> p.getActor() != null && p.getActor().hasReference()));

    if (!hasPatientReference) {
      String msg = "Appointment "
          + appointmentId
          + " (entry "
          + entryIndex
          + ") bevat geen patient-referentie (subject of participant)";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    // Syntax validatie: patient-referentie format
    FhirMessageValidationResult refValidation = validatePatientReferences(appointment, appointmentId, entryIndex);
    if (!refValidation.isValid()) {
      return refValidation;
    }

    log.debug("Appointment {} validatie succesvol", appointmentId);
    return FhirMessageValidationResult.valid();
  }

  /**
   * Valideert de patiënt-referenties in een Appointment (US-009
   * syntaxiscontrole).
   */
  private FhirMessageValidationResult validatePatientReferences(
      Appointment appointment, String appointmentId, int entryIndex) {
    if (appointment.hasSubject()) {
      String refValue = appointment.getSubject().getReference();
      if (refValue != null && !refValue.isBlank()) {
        if (!isValidFhirReference(refValue, "Patient")) {
          String msg = "Appointment " + appointmentId + " (entry " + entryIndex
              + ") bevat ongeldig subject reference format: " + refValue;
          log.warn(msg);
          return FhirMessageValidationResult.invalid(msg);
        }
      }
    }

    if (appointment.hasParticipant()) {
      for (Appointment.AppointmentParticipantComponent participant : appointment.getParticipant()) {
        if (participant.hasActor()) {
          String refValue = participant.getActor().getReference();
          if (refValue != null && !refValue.isBlank() && refValue.startsWith("Patient/")) {
            if (!isValidFhirReference(refValue, "Patient")) {
              String msg = "Appointment " + appointmentId + " (entry " + entryIndex
                  + ") bevat ongeldig participant reference format: " + refValue;
              log.warn(msg);
              return FhirMessageValidationResult.invalid(msg);
            }
          }
        }
      }
    }

    return FhirMessageValidationResult.valid();
  }

  /**
   * Valideert of een string een valide FHIR-referentie is.
   * Format: "ResourceType/id"
   */
  private boolean isValidFhirReference(String reference, String expectedResourceType) {
    if (reference == null || reference.isBlank()) {
      return false;
    }
    String[] parts = reference.split("/");
    if (parts.length != 2) {
      return false;
    }
    String resourceType = parts[0];
    String id = parts[1];

    if (!resourceType.equals(expectedResourceType)) {
      return false;
    }
    return isValidFhirId(id);
  }

  /**
   * Valideert of een string een valide FHIR-id is.
   * FHIR IDs mogen alphanumeriek zijn met enkele speciale karakters.
   */
  /**
   * US-009: minimaal één telecom met bruikbaar telefoonnummer (system phone/sms/other of leeg).
   */
  private boolean hasPhoneTelecom(Patient patient) {
    if (!patient.hasTelecom()) {
      return false;
    }
    return patient.getTelecom().stream().anyMatch(FhirMessageValidator::isUsablePhoneContact);
  }

  private static boolean isUsablePhoneContact(ContactPoint contactPoint) {
    if (!contactPoint.hasValue() || contactPoint.getValue().isBlank()) {
      return false;
    }
    if (!contactPoint.hasSystem()) {
      return true;
    }
    ContactPoint.ContactPointSystem system = contactPoint.getSystem();
    return system == ContactPoint.ContactPointSystem.PHONE
        || system == ContactPoint.ContactPointSystem.SMS
        || system == ContactPoint.ContactPointSystem.OTHER;
  }

  private static boolean hasBundleEntries(Bundle bundle) {
    return bundle.getEntry() != null && !bundle.getEntry().isEmpty();
  }

  private boolean isValidFhirId(String id) {
    if (id == null || id.isBlank()) {
      return false;
    }
    // FHIR IDs: 1-64 karakters, alleen [A-Za-z0-9\-\.]
    return id.matches("[A-Za-z0-9\\-\\.]{1,64}");
  }

  /**
   * Valideert een enkele FHIR Patient-resource (US-009 integratie voor polling).
   *
   * <p>
   * Dit is een convenience-methode voor het valideren van losse Patient-resources
   * in de polling-laag, zonder de volledige Bundle-validatie.
   *
   * @param patient De Patient-resource om te valideren
   * @return FhirMessageValidationResult met validatiestatus
   */
  public FhirMessageValidationResult validatePatientResource(Patient patient) {
    if (patient == null) {
      String msg = "Patient is null";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }
    // Gebruik entryIndex -1 omdat dit geen bundleentry is
    return validatePatient(patient, -1);
  }

  /**
   * Valideert een enkele FHIR Appointment-resource (US-009 integratie voor
   * polling).
   *
   * <p>
   * Dit is een convenience-methode voor het valideren van losse
   * Appointment-resources
   * in de polling-laag, zonder de volledige Bundle-validatie.
   *
   * @param appointment De Appointment-resource om te valideren
   * @return FhirMessageValidationResult met validatiestatus
   */
  public FhirMessageValidationResult validateAppointmentResource(Appointment appointment) {
    if (appointment == null) {
      String msg = "Appointment is null";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }
    // Gebruik entryIndex -1 omdat dit geen bundleentry is
    return validateAppointment(appointment, -1);
  }
}
