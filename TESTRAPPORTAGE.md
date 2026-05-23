# Testrapportage — OpenMRS Communicatiemodule

Versie 2.0 (finale) | Meting: 23 mei 2026

Deze rapportage beschrijft de **volledige automatische testdekking** van de finale communicatiemodule en hoe de **werking lokaal** reproduceerbaar is (zonder externe OpenMRS/FHIR/RabbitMQ in unit/integratietests). Sluit aan op deliverable 6 uit [docs/sprint3-doelen.txt](docs/sprint3-doelen.txt).

---

## Samenvatting

| Indicator | Resultaat |
|-----------|-----------|
| Totaal aantal tests | **269** |
| Geslaagd / mislukt / overgeslagen | **269 / 0 / 0** |
| Testklassen | **46** |
| Build | `mvnw test` — **BUILD SUCCESS** (~22 s) |
| Unit- en componenttests | **~237** (JUnit 5, Mockito en/of echte HAPI R5-componenten) |
| Spring-integratietests (`@SpringBootTest`, H2) | **~32** in 12 klassen |
| Provider-adapter unit-tests | **24** (4 providers) |
| FHIR US-009 / US-010 | **91** tests (validator, ACK/NACK, mapper, controller) |

---

## Tests uitvoeren (lokaal)

Geen Docker of externe services nodig voor de automatische suite.

```bash
./mvnw test
```

```powershell
.\mvnw.cmd test
```

Surefire-rapporten: `target/surefire-reports/`.

Optioneel alleen FHIR:

```bash
./mvnw test -Dtest="nl.openmrs.comm_module.messaging.fhir.*"
```

Optioneel alleen scheduling/notificaties:

```bash
./mvnw test -Dtest="nl.openmrs.comm_module.notification.*,nl.openmrs.comm_module.scheduling.*"
```

---

## Teststrategie

| Laag | Doel | Techniek |
|------|------|----------|
| **Unit** | Geïsoleerde businesslogica (scheduler, publisher, providers, consumer, encryptie, encoding) | JUnit 5 + Mockito |
| **Component-integratie** | Validator + mapper + sync-factory zonder Spring-context | Echte HAPI R5 `FhirContext`, geen mocks |
| **Spring-integratie** | Scheduler + delivery log + JPA in één context | `@SpringBootTest`, H2 in-memory, vaste `Clock`, gemockte FHIR/RabbitMQ waar nodig |
| **Lokaal handmatig (E2E)** | Volledige keten in Docker (distro → sync → HAPI → poll → scheduler → queue → fake provider) | Compose-stack, test-GUI, [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) |

Testconfiguratie: [src/test/resources/application.properties](src/test/resources/application.properties) — scheduler-listeners en Rabbit auto-startup uit waar nodig.

---

## Overzicht per domein

| Domein | Testklassen (voorbeeld) | Tests |
|--------|-------------------------|-------|
| FHIR berichten US-009/010 | `FhirMessageValidatorTest`, `FhirMessageAckServiceTest`, `FhirMessageControllerTest`, `FhirValidationIntegrationTest`, … | 91 |
| Character encoding | `CharacterEncodingServiceTest` | 51 |
| Scheduler & herinneringen | `NotificationSchedulerTest`, `DefaultDueNotificationProcessorTest`, `AppointmentReminder*Test`, integratieklassen | 38 |
| Messaging providers | `SwiftSendProviderTest`, `SecurePostProviderTest`, `LegacyLinkProviderTest`, `AsyncFlowProviderTest` | 24 |
| FHIR poll / sync / fallback | `FallbackAppointmentPollSourceTest`, `OpenmrsFhirResourceFactoryTest`, `AppointmentFhirMapperTest`, … | 35 |
| RabbitMQ consumer | `RabbitMqConsumerTest` | 6 |
| Organisatie-API | `OrganisationConfigControllerTest`, `OrganisationConfigServiceTest` | 8 |
| Poll-persistentie | `JpaAppointmentPollPersistenceTest` | 6 |
| Encryptie / TLS / config | `AesEncryptionServiceTest`, `OpenmrsFhirTlsApacheHttpClientTest`, properties-tests | 19 |
| Overige (retention, message log, test-GUI) | diverse | rest |

---

## 1. Scheduler en herinneringen

### Unit — `NotificationSchedulerTest`

| Test | Validatie |
|------|-----------|
| `roeptProcessorAanAlsSchedulerAanStaat` | Bij `enabled=true` wordt `processDueNotifications()` aangeroepen |
| `slaatOverAlsSchedulerUitStaat` | Bij `enabled=false` geen processor-aanroep |
| `vangtProcessorFoutOpZonderSchedulerTeStoppen` | Runtime-fout in processor stopt de scheduler-tick niet |

### Unit — `DefaultDueNotificationProcessorTest`

| Test | Validatie |
|------|-----------|
| `roeptQueryEnPublisherAanVoorElkeSpec` | Voor elke herinneringsspecificatie (24u en 1u) worden query en publisher aangeroepen |

### Spring-integratie — scheduling-flow

| Klasse | Tests | Validatie |
|--------|-------|-----------|
| `AppointmentReminderSchedulingIntegrationTest` | 3 | 24u-venster → publish + `QUEUED`; idempotentie bij tweede tick; scheduler uit → geen publish |
| `AppointmentReminder1HourSchedulingIntegrationTest` | 2 | 1u-lead naast 24u |
| `AppointmentCancellationIntegrationTest` | 2 | Geannuleerde afspraak → geen herinnering |

**Lokaal aantonen:** [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) en http://localhost:8081/test-scheduling.html

---

## 2. Provider-adapters en consumer

| Provider | Klasse | Tests |
|----------|--------|-------|
| SWIFTSEND | `SwiftSendProviderTest` | 7 |
| SECUREPOST | `SecurePostProviderTest` | 6 |
| LEGACYLINK | `LegacyLinkProviderTest` | 5 |
| ASYNCFLOW | `AsyncFlowProviderTest` | 6 |

`RabbitMqConsumerTest` (6 tests): logging na verzending, retry bij fout, geen retry na max, overslaan geannuleerde afspraak / ontbrekende QUEUED, weigering zonder requeue bij ontbrekende organisatieconfig.

---

## 3. Idempotentie

| Mechanisme | Testdekking |
|------------|-------------|
| Delivery log vóór publish | `NotificationDeliveryLogServiceTest`, `AppointmentReminderPublisherTest` |
| Geen dubbele publish bij scheduler-tick | `AppointmentReminderSchedulingIntegrationTest.tweedeSchedulerTickQueueNietOpnieuwNaEerstePoging` |
| Consumer alleen bij QUEUED-record | `RabbitMqConsumerTest.slaatVerzendingOverAlsQueuedBijAnnuleringWegIs` |

---

## 4. FHIR R5 — validatie, ACK/NACK en mapping (US-009 / US-010)

| Klasse | Tests | Validatie |
|--------|-------|-----------|
| `FhirMessageValidatorTest` | 27 | Verplichte velden, Bundle-structuur, Patient/Appointment-regels |
| `FhirMessageAckServiceTest` | 18 | ACK/NACK `OperationOutcome` |
| `FhirMessageControllerTest` | 8 | HTTP 200/400, happy path en foutpaden |
| `FhirMessageControllerWebMvcTest` | 5 | WebMvc + content-type |
| `FhirValidationIntegrationTest` | 7 | Validator + mapper + factory zonder mocks |
| `FhirMapperValidatorIntegrationTest` | 12 | End-to-end validatie op echte resources |
| `FhirMessageProcessorTest` | 3 | Verwerking na validatie |
| `PatientFhirMapperTest` / `AppointmentFhirMapperTest` | 13 | DTO ↔ FHIR R5 |

Beheerdersregels: [docs/fhir-validatie-beheerders.md](docs/fhir-validatie-beheerders.md).

---

## 5. FHIR-poll, sync en tijdzone

| Klasse | Tests | Validatie |
|--------|-------|-----------|
| `FallbackAppointmentPollSourceTest` | 3 | FHIR primair; JDBC-fallback bij exception |
| `OpenmrsPollOrganisationScopeTest` | 4 | FHIR-URL per organisatie |
| `OpenmrsFhirResourceFactoryTest` | 3 | `dbZone` UTC → correcte weergave Amsterdam |
| `OpenmrsSchedulingSyncPropertiesTest` | 2 | Naive UTC → `Europe/Amsterdam` |
| `OpenmrsSchedulingStatusMapperTest` | 3 | OpenMRS-status → FHIR R5 |
| `RetryingOpenmrsFhirOperationsTest` | 3 | Retry bij FHIR-fouten |
| `OpenmrsFhirPropertiesTest` | 5 | Poll-modus en fallback-flags |

---

## 6. Overige Spring-integratietests

| Klasse | Tests | Doel |
|--------|-------|------|
| `CommModuleApplicationTests` | 1 | Context start |
| `NotificationDeliveryLogServiceTest` | 3 | QUEUED/SENT/FAILED, annulering |
| `JpaAppointmentPollPersistenceTest` | 6 | `polled_appointment` JPA |
| `AppointmentReminderQueryServiceTest` | 5 | Due-query met vaste tijd |
| `MessageLogRetentionServiceTest` | 1 | Retention-job |
| `DataRetentionServiceTest` | 2 | Dataretentie |

---

## Lokaal demonstreren (checklist E2E)

Vereist: OpenMRS 3 distro + comm-module compose (zie [README.md](README.md)).

| # | Stap | Verwacht resultaat |
|---|------|-------------------|
| 1 | `docker compose up -d --build` | `comm-module-app` **healthy**; HAPI op 8082: `/fhir/metadata` |
| 2 | `GET http://localhost:8081/actuator/health` | `"status":"UP"` |
| 3 | `POST http://localhost:8081/api/fhir/messages` (geldige Bundle) | HTTP **200**, `OperationOutcome` ACK |
| 4 | `GET http://localhost:8081/api/test/scheduling/status` | Scheduler/poll-config zichtbaar |
| 5 | Scheduling-test [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) | Sync → poll → delivery log `QUEUED` → `SENT` |
| 6 | Tweede scheduler-tick | Geen dubbele herinnering (idempotentie) |

Test-GUI: http://localhost:8081/test-scheduling.html

---

## Conclusie

De finale module heeft **269** automatische tests die **lokaal met één Maven-commando** draaien. Kernlogica (scheduler, providers, idempotentie), de **FHIR R5-keten** (validatie, ACK/NACK, poll/sync) en **organisatieconfiguratie** zijn afgedekt. De **Docker-stack** en het scheduling-testplan maken de productieketen reproduceerbaar voor beheerders en beoordeling.

Zie ook: [PERFORMANCERAPPORTAGE.md](PERFORMANCERAPPORTAGE.md).
