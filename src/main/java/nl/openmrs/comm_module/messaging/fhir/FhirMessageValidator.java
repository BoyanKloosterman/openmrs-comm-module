package nl.openmrs.comm_module.messaging.fhir;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
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

    if (!bundle.hasEntry() || bundle.getEntry().isEmpty()) {
      log.warn("FHIR Bundle bevat geen entries");
      return FhirMessageValidationResult.invalid("FHIR Bundle bevat geen entries");
    }

    // Valideer elk entry in de bundle
    int entryIndex = 0;
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource() == null) {
        log.warn("Entry {} bevat geen resource", entryIndex);
        return FhirMessageValidationResult.invalid(
            "Entry " + entryIndex + " bevat geen resource");
      }

      // Valideer Patient entries
      if (entry.getResource() instanceof Patient patient) {
        FhirMessageValidationResult patientValidation = validatePatient(patient, entryIndex);
        if (!patientValidation.isValid()) {
          return patientValidation;
        }
      }

      // Valideer Appointment entries
      if (entry.getResource() instanceof Appointment appointment) {
        FhirMessageValidationResult appointmentValidation = validateAppointment(appointment, entryIndex);
        if (!appointmentValidation.isValid()) {
          return appointmentValidation;
        }
      }

      entryIndex++;
    }

    log.debug("FHIR Bundle validatie succesvol");
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
    if (!patient.hasId() || patient.getId().isBlank()) {
      String msg = "Patient (entry " + entryIndex + ") bevat geen id";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!patient.hasName() || patient.getName().isEmpty()) {
      String msg = "Patient " + patient.getId() + " (entry " + entryIndex + ") bevat geen name";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    boolean hasValidName = patient.getName().stream()
        .anyMatch(name -> name.hasGiven() || name.hasFamily());
    if (!hasValidName) {
      String msg = "Patient "
          + patient.getId()
          + " (entry "
          + entryIndex
          + ") bevat geen gegeven of familienaam";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!patient.hasGender() && !patient.hasBirthDate()) {
      String msg = "Patient "
          + patient.getId()
          + " (entry "
          + entryIndex
          + ") bevat geen gender en geen birthDate";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!patient.hasTelecom() || patient.getTelecom().isEmpty()) {
      String msg = "Patient "
          + patient.getId()
          + " (entry "
          + entryIndex
          + ") bevat geen telecom (geen contact informatie)";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    log.debug("Patient {} validatie succesvol", patient.getId());
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
    if (!appointment.hasId() || appointment.getId().isBlank()) {
      String msg = "Appointment (entry " + entryIndex + ") bevat geen id";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    if (!appointment.hasStart()) {
      String msg = "Appointment "
          + appointment.getId()
          + " (entry "
          + entryIndex
          + ") bevat geen start (afspraaktijd)";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    // Check voor patient-referentie via subject of participant
    boolean hasPatientReference = (appointment.hasSubject() && appointment.getSubject().hasReference())
        || (appointment.hasParticipant()
            && appointment.getParticipant().stream()
                .anyMatch(p -> p.getActor() != null && p.getActor().hasReference()));

    if (!hasPatientReference) {
      String msg = "Appointment "
          + appointment.getId()
          + " (entry "
          + entryIndex
          + ") bevat geen patient-referentie (subject of participant)";
      log.warn(msg);
      return FhirMessageValidationResult.invalid(msg);
    }

    log.debug("Appointment {} validatie succesvol", appointment.getId());
    return FhirMessageValidationResult.valid();
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
