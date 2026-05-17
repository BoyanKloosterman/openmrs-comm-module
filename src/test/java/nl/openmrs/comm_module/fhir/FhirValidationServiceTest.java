package nl.openmrs.comm_module.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FhirValidationService - Encounter-validatie")
class FhirValidationServiceTest {

  private FhirValidationService validationService;
  private IParser parser;
  private final String testResourcesPath = "src/test/resources/fhir/";

  @BeforeEach
  void setUp() {
    validationService = new FhirValidationService();
    FhirContext fhirContext = FhirContext.forR4();
    parser = fhirContext.newJsonParser();
  }

  @Test
  @DisplayName("Scenario 1: Geldige Encounter met status, start, en participant")
  void testValidEncounterWithAllRequiredFields() throws IOException {
    // Arrange
    String json = readTestResource("encounter-valid.json");
    Encounter encounter = parser.parseResource(Encounter.class, json);

    // Act
    Optional<Encounter> result = validationService.validateEncounter(encounter);

    // Assert
    assertTrue(result.isPresent(), "Geldige encounter moet geaccepteerd worden");
    assertNotNull(result.get().getId());
  }

  @Test
  @DisplayName("Scenario 2: Ongeldige Encounter zonder participant")
  void testEncounterWithoutParticipantIsInvalid() throws IOException {
    // Arrange
    String json = readTestResource("encounter-missing-participant.json");
    Encounter encounter = parser.parseResource(Encounter.class, json);

    // Act
    Optional<Encounter> result = validationService.validateEncounter(encounter);

    // Assert
    assertTrue(result.isEmpty(), "Encounter zonder participant moet geweigerd worden");
  }

  @Test
  @DisplayName("Scenario 3: Ongeldige Encounter met cancelled status")
  void testEncounterWithCancelledStatusIsInvalid() throws IOException {
    // Arrange
    String json = readTestResource("encounter-cancelled.json");
    Encounter encounter = parser.parseResource(Encounter.class, json);

    // Act
    Optional<Encounter> result = validationService.validateEncounter(encounter);

    // Assert
    assertTrue(result.isEmpty(), "Encounter met cancelled status moet geweigerd worden");
  }

  @Test
  @DisplayName("Scenario 4: Ongeldige Encounter zonder status")
  void testEncounterWithoutStatusIsInvalid() throws IOException {
    // Arrange
    String json = readTestResource("encounter-missing-status.json");
    Encounter encounter = parser.parseResource(Encounter.class, json);

    // Act
    Optional<Encounter> result = validationService.validateEncounter(encounter);

    // Assert
    assertTrue(result.isEmpty(), "Encounter zonder status moet geweigerd worden");
  }

  @Test
  @DisplayName("Scenario 5: Ongeldige Encounter zonder start in period")
  void testEncounterWithoutPeriodStartIsInvalid() throws IOException {
    // Arrange
    String json = readTestResource("encounter-missing-start.json");
    Encounter encounter = parser.parseResource(Encounter.class, json);

    // Act
    Optional<Encounter> result = validationService.validateEncounter(encounter);

    // Assert
    assertTrue(result.isEmpty(), "Encounter zonder period.start moet geweigerd worden");
  }

  @Test
  @DisplayName("Null Encounter wordt geweigerd")
  void testNullEncounterIsInvalid() {
    // Act
    Optional<Encounter> result = validationService.validateEncounter(null);

    // Assert
    assertTrue(result.isEmpty(), "Null encounter moet geweigerd worden");
  }

  /**
   * Leest een JSON test resource uit de fhir map.
   */
  private String readTestResource(String filename) throws IOException {
    String path = testResourcesPath + filename;
    return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
  }
}
