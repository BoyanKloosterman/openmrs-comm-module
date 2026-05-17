package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.fhir.FhirValidationService;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.poll.EncounterPollPersistence;
import nl.openmrs.comm_module.messaging.fhir.EncounterFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.PatientFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenmrsFhirPollingService {

  private static final Logger log = LoggerFactory.getLogger(OpenmrsFhirPollingService.class);
  private final OpenmrsFhirOperations fhirOperations;
  private final FhirValidationService fhirValidationService;
  private final EncounterFhirMapper encounterFhirMapper;
  private final PatientFhirMapper patientFhirMapper;
  private final EncounterPollPersistence encounterPollPersistence;
  private final OpenmrsFhirProperties fhirProperties;

  public OpenmrsFhirPollingService(
      OpenmrsFhirOperations fhirOperations,
      FhirValidationService fhirValidationService,
      EncounterFhirMapper encounterFhirMapper,
      PatientFhirMapper patientFhirMapper,
      EncounterPollPersistence encounterPollPersistence,
      OpenmrsFhirProperties fhirProperties) {
    this.fhirOperations = fhirOperations;
    this.fhirValidationService = fhirValidationService;
    this.encounterFhirMapper = encounterFhirMapper;
    this.patientFhirMapper = patientFhirMapper;
    this.encounterPollPersistence = encounterPollPersistence;
    this.fhirProperties = fhirProperties;
  }

  @Scheduled(fixedDelayString = "#{@openmrsFhirProperties.pollDelayMillis()}")
  public void pollOpenmrsFhir() {
    log.debug("FHIR poll gestart");
    try {
      String info = fhirOperations.fetchServerSoftwareNameAndVersion();
      log.info("FHIR server: {}", info);

      String since = LocalDate.now(ZoneOffset.UTC)
          .minusDays(Math.max(0, fhirProperties.getEncounterPollSinceDays()))
          .toString();
      Bundle bundle = fhirOperations.searchEncountersSince(since);
      List<EncounterPollDto> snapshots = mapBundle(bundle);
      List<EncounterWithPatientDto> withPatients = attachPatients(snapshots);

      long metPatient = withPatients.stream().filter(e -> e.patient() != null).count();
      log.info(
          "Encounter-poll: {} bundle-entries, {} encounters, {} met Patient (since={})",
          bundle.hasEntry() ? bundle.getEntry().size() : 0,
          snapshots.size(),
          metPatient,
          since);

      encounterPollPersistence.upsertPollResults(fhirProperties.getOrganisationId(), withPatients);
    } catch (RuntimeException e) {
      // Na retries in RetryingOpenmrsFhirOperations: scheduler mag niet crashen
      log.error("FHIR poll mislukt: {}", e.getMessage(), e);
    }
  }

  /**
   * Zet bundle entries om naar DTO's; niet-Encounter entries worden overgeslagen.
   * Valideert verplichte velden (status, start, participant) en filtert ongeldige
   * resources.
   */
  private List<EncounterPollDto> mapBundle(Bundle bundle) {
    List<EncounterPollDto> out = new ArrayList<>();
    if (bundle == null || !bundle.hasEntry()) {
      return out;
    }
    for (var entry : bundle.getEntry()) {
      if (entry == null || !entry.hasResource()) {
        continue;
      }
      Resource resource = entry.getResource();
      if (!(resource instanceof Encounter encounter)) {
        continue;
      }
      // Valideer encounter voordat we deze mappen
      if (fhirValidationService.validateEncounter(encounter).isPresent()) {
        encounterFhirMapper.mapEncounterToEncounterPollDto(encounter).ifPresent(out::add);
      }
    }
    return out;
  }

  /**
   * Haalt per unieke patientId één keer de Patient op (FHIR read) en koppelt aan
   * encounter.
   * Bij ontbrekende Patient: patient=null en warn-log.
   */
  private List<EncounterWithPatientDto> attachPatients(List<EncounterPollDto> encounters) {
    Map<String, Optional<PatientPollDto>> cache = new HashMap<>();
    List<EncounterWithPatientDto> out = new ArrayList<>(encounters.size());
    for (EncounterPollDto enc : encounters) {
      String pid = enc.patientId();
      Optional<PatientPollDto> patientOpt = cache.computeIfAbsent(pid, this::loadPatientPollDto);
      PatientPollDto patient = patientOpt.orElse(null);
      if (patient == null) {
        log.warn("Patient niet geladen voor encounter {} (patientId={})", enc.encounterId(), pid);
      }
      out.add(new EncounterWithPatientDto(enc, patient));
    }
    return out;
  }

  private Optional<PatientPollDto> loadPatientPollDto(String patientLogicalId) {
    return fhirOperations
        .readPatientByLogicalId(patientLogicalId)
        .flatMap(patientFhirMapper::mapPatient);
  }
}
