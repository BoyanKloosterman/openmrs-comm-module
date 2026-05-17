package nl.openmrs.comm_module.scheduling;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import nl.openmrs.comm_module.fhir.FhirValidationService;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.EncounterFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.PatientFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterPollDto;
import nl.openmrs.comm_module.poll.EncounterPollPersistence;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenmrsFhirPollingService - Validatie-integratie")
class OpenmrsFhirPollingServiceValidationTest {

  private OpenmrsFhirPollingService pollingService;
  private FhirValidationService fhirValidationService;
  private IParser parser;

  @Mock
  private OpenmrsFhirOperations fhirOperations;

  @Mock
  private EncounterFhirMapper encounterFhirMapper;

  @Mock
  private PatientFhirMapper patientFhirMapper;

  @Mock
  private EncounterPollPersistence encounterPollPersistence;

  @Mock
  private OpenmrsFhirProperties fhirProperties;

  private final String testResourcesPath = "src/test/resources/fhir/";

  @BeforeEach
  void setUp() {
    fhirValidationService = new FhirValidationService();
    pollingService = new OpenmrsFhirPollingService(
        fhirOperations,
        fhirValidationService,
        encounterFhirMapper,
        patientFhirMapper,
        encounterPollPersistence,
        fhirProperties);

    FhirContext fhirContext = FhirContext.forR4();
    parser = fhirContext.newJsonParser();

    // Mock-configuratie
    when(fhirProperties.getOrganisationId()).thenReturn("org-1");
    when(fhirProperties.getEncounterPollSinceDays()).thenReturn(7);
  }

  @Test
  @DisplayName("Geldige encounters worden naar scheduling-laag doorgegeven")
  void testValidEncountersPassed() throws IOException {
    // Arrange
    String validJson = readTestResource("encounter-valid.json");
    Encounter encounter = parser.parseResource(Encounter.class, validJson);

    Bundle bundle = new Bundle();
    BundleEntryComponent entry = new BundleEntryComponent();
    entry.setResource(encounter);
    bundle.addEntry(entry);

    when(fhirOperations.fetchServerSoftwareNameAndVersion()).thenReturn("OpenMRS 2.x");
    when(fhirOperations.searchEncountersSince(anyString())).thenReturn(bundle);
    when(encounterFhirMapper.mapEncounterToEncounterPollDto(any(Encounter.class)))
        .thenReturn(java.util.Optional.of(
            new EncounterPollDto(
                "uuid-1", "enc-1", "pat-42",
                java.time.Instant.now(), "loc-1", "Visit", false)));

    // Act
    pollingService.pollOpenmrsFhir();

    // Assert
    verify(encounterPollPersistence, times(1)).upsertPollResults(
        eq("org-1"),
        argThat(list -> !list.isEmpty()));
  }

  @Test
  @DisplayName("Ongeldige encounters worden gefilterd en niet doorgegeven")
  void testInvalidEncountersFiltered() throws IOException {
    // Arrange - encounter zonder participant
    String invalidJson = readTestResource("encounter-missing-participant.json");
    Encounter encounter = parser.parseResource(Encounter.class, invalidJson);

    Bundle bundle = new Bundle();
    BundleEntryComponent entry = new BundleEntryComponent();
    entry.setResource(encounter);
    bundle.addEntry(entry);

    when(fhirOperations.fetchServerSoftwareNameAndVersion()).thenReturn("OpenMRS 2.x");
    when(fhirOperations.searchEncountersSince(anyString())).thenReturn(bundle);

    // Act
    pollingService.pollOpenmrsFhir();

    // Assert - mapper mag niet eens opgeroepen worden
    verify(encounterFhirMapper, never()).mapEncounterToEncounterPollDto(any());
    verify(encounterPollPersistence, times(1)).upsertPollResults(
        eq("org-1"),
        argThat(List::isEmpty) // Lege lijst
    );
  }

  @Test
  @DisplayName("Mix van geldige en ongeldige encounters: alleen geldige doorgegeven")
  void testMixedValidAndInvalidEncounters() throws IOException {
    // Arrange
    String validJson = readTestResource("encounter-valid.json");
    String invalidJson = readTestResource("encounter-missing-participant.json");

    Encounter validEncounter = parser.parseResource(Encounter.class, validJson);
    Encounter invalidEncounter = parser.parseResource(Encounter.class, invalidJson);

    Bundle bundle = new Bundle();
    BundleEntryComponent validEntry = new BundleEntryComponent();
    validEntry.setResource(validEncounter);
    BundleEntryComponent invalidEntry = new BundleEntryComponent();
    invalidEntry.setResource(invalidEncounter);

    bundle.addEntry(validEntry);
    bundle.addEntry(invalidEntry);

    when(fhirOperations.fetchServerSoftwareNameAndVersion()).thenReturn("OpenMRS 2.x");
    when(fhirOperations.searchEncountersSince(anyString())).thenReturn(bundle);
    when(encounterFhirMapper.mapEncounterToEncounterPollDto(any(Encounter.class)))
        .thenReturn(java.util.Optional.of(
            new EncounterPollDto(
                "uuid-1", "enc-1", "pat-42",
                java.time.Instant.now(), "loc-1", "Visit", false)));

    // Act
    pollingService.pollOpenmrsFhir();

    // Assert - mapper moet exact 1 keer gecalled worden (voor de geldige)
    verify(encounterFhirMapper, times(1)).mapEncounterToEncounterPollDto(any());
    verify(encounterPollPersistence, times(1)).upsertPollResults(
        eq("org-1"),
        argThat(list -> list.size() == 1) // Exact 1 encounter
    );
  }

  /**
   * Leest een JSON test resource uit de fhir map.
   */
  private String readTestResource(String filename) throws IOException {
    String path = testResourcesPath + filename;
    return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
  }
}
