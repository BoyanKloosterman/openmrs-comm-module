package nl.openmrs.comm_module.fhir;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Valideert FHIR Encounter resources op verplichte velden:
 * - status: moet aanwezig zijn en niet "cancelled" of "entered-in-error"
 * - start: moet aanwezig zijn in period
 * - participant: moet minstens één participant bevatten
 */
@Component
public class FhirValidationService {

  private static final Logger log = LoggerFactory.getLogger(FhirValidationService.class);

  /**
   * Valideert een FHIR Encounter resource.
   *
   * @param encounter de te valideren Encounter
   * @return Optional.of(encounter) als valid, Optional.empty() als invalid
   */
  public Optional<Encounter> validateEncounter(Encounter encounter) {
    if (encounter == null) {
      log.error("Encounter validation mislukt: encounter is null");
      return Optional.empty();
    }

    // Validatie 1: status moet aanwezig zijn
    if (!encounter.hasStatus()) {
      log.error("Encounter validation mislukt voor ID {}: status ontbreekt", encounter.getId());
      return Optional.empty();
    }

    // Validatie 2: status mag niet "cancelled" of "entered-in-error" zijn
    EncounterStatus status = encounter.getStatus();
    if (status == EncounterStatus.CANCELLED || status == EncounterStatus.ENTEREDINERROR) {
      log.error(
          "Encounter validation mislukt voor ID {}: status is {} (invalid)",
          encounter.getId(),
          status.toCode());
      return Optional.empty();
    }

    // Validatie 3: period.start moet aanwezig zijn
    if (!encounter.hasPeriod() || !encounter.getPeriod().hasStart()) {
      log.error("Encounter validation mislukt voor ID {}: period.start ontbreekt", encounter.getId());
      return Optional.empty();
    }

    // Validatie 4: minstens één participant moet aanwezig zijn
    if (!encounter.hasParticipant() || encounter.getParticipant().isEmpty()) {
      log.error("Encounter validation mislukt voor ID {}: participant ontbreekt", encounter.getId());
      return Optional.empty();
    }

    log.debug("Encounter ID {} succesvol gevalideerd", encounter.getId());
    return Optional.of(encounter);
  }
}
