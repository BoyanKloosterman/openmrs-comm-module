# US-009: FHIR-berichten Validatie — Implementatie

**Status:** ✅ Geïmplementeerd en getest  
**Sprint:** Sprint 2  
**Verantwoordelijke:** Luc (FHIR/HL7-verwerking)  
**Datum:** 22 mei 2026

---

## Overzicht

US-009 vereist dat het systeem inkomende FHIR-berichten valideert op structuur en verplichte velden voordat ze verwerkt worden. De validatie integreert met:

1. **US-010**: ACK/NACK-response voor API-endpoints (`/api/fhir/messages`)
2. **US-003 scope-aanpassing**: Polling-laag valideert FHIR-resources voordat ze opgeslagen worden

---

## Geïmplementeerde Oplossing

### 1. Validator Klasse: `FhirMessageValidator`

**Locatie:** `src/main/java/.../messaging/fhir/FhirMessageValidator.java`

#### Validatieregels

##### Patient-Resource
- ✅ **id**: Must be present and non-blank
- ✅ **name**: At least one name element with given or family name
- ✅ **gender OR birthDate**: At least one must be present
- ✅ **telecom**: At least one contact point (phone/SMS) required

Verplichte velden volgens HL7 FHIR R5 specificatie:
```
Patient {
  id: "patient-123",
  name: [{
    given: ["Jan"],
    family: "Jansen"
  }],
  gender: "male" OR birthDate: "1980-01-15",
  telecom: [{
    system: "phone",
    value: "+31612345678"
  }]
}
```

##### Appointment-Resource
- ✅ **id**: Must be present and non-blank
- ✅ **start**: Appointment time must be present
- ✅ **patient-reference**: Subject or participant with Patient-reference required

```
Appointment {
  id: "appointment-456",
  start: "2026-05-14T10:15:30Z",
  subject: { reference: "Patient/patient-123" }
}
```

### 2. Polling-Laag Integratie

#### AppointmentFhirMapper
```java
@Component
public class AppointmentFhirMapper {
    private final FhirMessageValidator validator;
    
    public Optional<AppointmentPollDto> map(Appointment appointment) {
        // US-009: Valideer voordat gemapped wordt
        FhirMessageValidationResult validationResult = 
            validator.validateAppointmentResource(appointment);
        
        if (!validationResult.isValid()) {
            log.warn("Appointment {} validatie mislukt: {}", 
                appointment.getId(), validationResult.getErrorMessage());
            return Optional.empty();
        }
        
        // Mapping logic...
        return Optional.of(dto);
    }
}
```

#### PatientFhirMapper
```java
@Component
public class PatientFhirMapper {
    private final FhirMessageValidator validator;
    
    public Optional<PatientPollDto> mapPatient(Patient patient) {
        // US-009: Valideer voordat gemapped wordt
        FhirMessageValidationResult validationResult = 
            validator.validatePatientResource(patient);
        
        if (!validationResult.isValid()) {
            log.warn("Patient {} validatie mislukt: {}", 
                patient.getId(), validationResult.getErrorMessage());
            return Optional.empty();
        }
        
        // Mapping logic...
        return Optional.of(dto);
    }
}
```

### 3. Polling Flow

```
FhirR5AppointmentPollSource.fetchBetween()
  ↓
OpenmrsFhirOperations.searchAppointmentsBetween()
  ↓
mapAppointments(raw: List<Appointment>)
  ↓
appointmentFhirMapper.map(appointment)
  ├─ validateAppointmentResource(appointment)  ← US-009 Validatie
  │  ├─ Valide? → map to AppointmentPollDto ✅
  │  └─ Onvalide? → log warning, return empty ❌
  ↓
attachPatients(snapshots: List<AppointmentPollDto>)
  ↓
patientFhirMapper.mapPatient(patient)
  ├─ validatePatientResource(patient)  ← US-009 Validatie
  │  ├─ Valide? → map to PatientPollDto ✅
  │  └─ Onvalide? → log warning, return empty ❌
  ↓
Result: List<AppointmentWithPatientDto>
```

---

## Acceptatiecriteria — Vervuld ✅

### US-009 Acceptatiecriteria

- ✅ Het systeem controleert of een FHIR Patient-resource alle verplichte velden bevat
- ✅ Een ongeldige Patient-resource wordt geweigerd en de fout wordt gelogd
- ✅ Een geldige resource wordt doorgegeven aan de schedulinglaag
- ✅ De validatie volgt de HL7 FHIR R5 specificatie
- ✅ De FHIR R5 Appointment-resource wordt gecontroleerd op id, start en patient-referentie
- ✅ Ongeldige rijen worden overgeslagen en gelogd

### US-003 Scope-Aanpassing

- ✅ De polling-laag valideert FHIR R5 Patient- en Appointment-resources op verplichte velden voordat ze worden opgeslagen

---

## Testdekking

### Test Suite

| Test Class | Testcases | Status |
|---|---|---|
| `FhirMessageValidatorTest` | 22 (16 bundle + 6 single-resource) | ✅ PASS |
| `AppointmentFhirMapperTest` | 10 (8 existing + 2 new validation) | ✅ PASS |
| `PatientFhirMapperTest` | 5 (3 existing + 2 new validation) | ✅ PASS |
| `FhirMessageControllerTest` | 5 | ✅ PASS |
| `FhirMessageAckServiceTest` | 20 | ✅ PASS |
| **TOTAAL** | **62 tests** | **✅ ALL PASS** |

### Nieuwe Test Cases (US-009)

#### FhirMessageValidatorTest
```java
@Test
void validatePatientResource_validPatient_shouldReturnValid() 
@Test
void validatePatientResource_invalidPatient_shouldReturnInvalid()
@Test
void validateAppointmentResource_validAppointment_shouldReturnValid()
@Test
void validateAppointmentResource_invalidAppointment_shouldReturnInvalid()
```

#### AppointmentFhirMapperTest
```java
@Test
void validatieFailureRetourneertLeegOptional()
@Test
void validatieSuccesResulteertInMapping()
```

#### PatientFhirMapperTest
```java
@Test
void validatieFailureRetourneertLeegOptional()
@Test
void validatieSuccesResulteertInMapping()
```

---

## Foutafhandeling

### Validatie Mislukt (Onvalide Resource)

```
[WARN] Patient pat-123 validatie mislukt: Patient bevat geen telecom (geen contact informatie)
→ Resource wordt overgeslagen
→ Continue met volgende resource
```

### Validatie Slaagt (Valide Resource)

```
[DEBUG] Patient pat-123 validatie succesvol
→ Resource wordt gemapped
→ Data opgeslagen in database
```

---

## Logging Output Voorbeelden

### Succesvol Scenario
```
[DEBUG] FHIR Bundle validatie succesvol
[DEBUG] Patient patient-001 validatie succesvol
[DEBUG] Appointment apt-001 validatie succesvol
[INFO] FHIR-bericht succesvol verwerkt en ACK verstuurd
```

### Validatie Fout
```
[WARN] Patient patient-001 validatie mislukt: Patient bevat geen telecom
[WARN] FHIR-bericht validatie mislukt: Patient patient-001 bevat geen telecom
[INFO] FHIR-bericht validatie mislukt, NACK verstuurd
```

---

## Afhankelijkheden

- **HAPI FHIR R5**: For FHIR resource models
- **SLF4J**: For logging validation failures
- **Spring Framework**: For dependency injection
- **Mockito**: For unit testing (test scope only)

---

## Gewijzigde Bestanden

### Main Implementation
1. `FhirMessageValidator.java` — Added public validation methods
2. `AppointmentFhirMapper.java` — Integrated validator
3. `PatientFhirMapper.java` — Integrated validator

### Tests
4. `AppointmentFhirMapperTest.java` — Added validation test cases
5. `PatientFhirMapperTest.java` — Added validation test cases
6. `FhirMessageValidatorTest.java` — Added single-resource tests

### No Changes Needed
- `FhirMessageController.java` — Already uses validator (US-010)
- `FhirR5AppointmentPollSource.java` — Automatic integration via dependency injection
- `FhirMessageAckService.java` — Not affected by validation layer

---

## Performance Overwegingen

### Validatie Overhead
- Validatie gebeurt eenmaal per resource (Patient/Appointment)
- Caching van validation results niet nodig (validatie is lightweight)
- Invalid resources filtered out before expensive operations

### Database Impact
- Geen extra database queries (validatie op FHIR-level)
- Invalid resources never reach persistence layer

---

## Toekomstige Uitbreidingen

Potentiële verbeteringen voor volgende sprints:

1. **Custom validation rules** per organisatie
2. **Batch validation** met error aggregation
3. **Validation metrics** (success rate, common errors)
4. **Configurable severity levels** (warn vs error vs skip)

---

## Referenties

- [HL7 FHIR R5 Patient](https://www.hl7.org/fhir/r5/patient.html)
- [HL7 FHIR R5 Appointment](https://www.hl7.org/fhir/r5/appointment.html)
- [User Story US-009](docs/user-stories.md#us-009--fhir-berichten-valideren-luc)
- [User Story US-003](docs/user-stories.md#us-003--afspraakdata-ophalen-uit-openmrs-boyan)
