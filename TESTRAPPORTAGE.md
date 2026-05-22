# Testrapportage — OpenMRS Communicatiemodule

Versie 1.1 | Sprint 3 | Meting: 22 mei 2026

Deze rapportage beschrijft hoe de **kernlogica** van de module is afgedekt met geautomatiseerde tests en hoe de **werking lokaal** kan worden aangetoond. Het sluit aan op deliverable 6 uit [docs/sprint3-doelen.txt](docs/sprint3-doelen.txt).

---

## Samenvatting

| Indicator | Resultaat |
|-----------|-----------|
| Totaal aantal tests | **141** |
| Geslaagd / mislukt / overgeslagen | **141 / 0 / 0** |
| Build | `mvnw test` — **BUILD SUCCESS** (~20 s) |
| Integratietests (`@SpringBootTest`) | **22** tests in **7** klassen |
| Provider-adapter unit-tests | **20** tests (4 providers) |

---

## Teststrategie

| Laag | Doel | Techniek |
|------|------|----------|
| **Unit** | Geïsoleerde businesslogica (scheduler, publisher, providers, consumer, poll-config) | JUnit 5 + Mockito |
| **Integratie** | Scheduler + delivery log + DB + publisher + poll-persistentie in één Spring-context | `@SpringBootTest`, vaste `Clock`, gemockte FHIR/RabbitMQ/JDBC waar nodig |
| **Lokaal handmatig** | End-to-end keten in Docker (distro-afspraak → poll → scheduler → queue → fake provider) | Compose + distro, logmonitor, [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) |

Automatische tests draaien zonder externe OpenMRS/FHIR/RabbitMQ (test-`application.properties` schakelt scheduler-listeners en Rabbit auto-startup uit waar nodig).

---

## 1. Scheduler

De scheduler (`NotificationScheduler`) roept periodiek `DueNotificationProcessor` aan. Gedrag wordt op drie niveaus getest.

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

### Integratie — scheduling-flow

| Klasse | Tests | Validatie |
|--------|-------|-----------|
| `AppointmentReminderSchedulingIntegrationTest` | 3 | Afspraak in 24u-venster → publish + `QUEUED`; **idempotentie** bij tweede tick; scheduler uit → geen publish |
| `AppointmentReminder1HourSchedulingIntegrationTest` | 2 | Aparte 1u-lead naast 24u |
| `AppointmentCancellationIntegrationTest` | 2 | Geannuleerde/voided afspraak → geen herinnering |
| `AppointmentReminderQueryServiceTest` | 5 | Query op venster en eligibility (Spring + DB) |
| `NotificationDeliveryLogServiceTest` | 3 | Delivery-log gedrag in context |

**Lokaal aantonen:** [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) — scheduler-tick in logs, delivery log `QUEUED` → `SENT`, tweede tick zonder dubbele queue.

---

## 2. Provider-adapter

Elke messaging-provider implementeert `MessagingProvider` en wordt via `MessagingProviderFactory` door `RabbitMqConsumer` aangeroepen.

### Unit-tests per provider

| Provider | Testklasse | Tests | Dekking (kern) |
|----------|------------|-------|----------------|
| SWIFTSEND | `SwiftSendProviderTest` | 6 | Succes, null-response, gedeeltelijke failures, API-exception |
| SECUREPOST | `SecurePostProviderTest` | 5 | Token + verzending, foutafhandeling |
| LEGACYLINK | `LegacyLinkProviderTest` | 4 | SOAP-mapping, succes/fout |
| ASYNCFLOW | `AsyncFlowProviderTest` | 5 | Submit + status polling |

### Consumer — koppeling adapter ↔ log

`RabbitMqConsumerTest` (5 tests):

| Test | Validatie |
|------|-----------|
| `logtVerzendstatusNaProviderPoging` | Na `sendMessage` wordt poging gelogd |
| `retryBijMisluktePoging` | Mislukking → opnieuw op retry-queue (tot max pogingen) |
| `geenRetryNaMaxPogingen` | Na max → `AmqpRejectAndDontRequeueException` |
| Overige | Geen dubbele verwerking zonder `QUEUED`-record; voided appointment wordt overgeslagen |

**Lokaal aantonen:**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/notifications/test"
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/notifications/test?provider=SECUREPOST"
```

Controle: RabbitMQ Management (queue-diepte), logs `comm-module`, rijen in `notification_delivery_log`.

---

## 3. Idempotentie

Idempotentie voorkomt dubbele herinneringen voor dezelfde afspraak en berichttype. Mechanismen:

1. **Delivery log** — `NotificationDeliveryLogService.hasSuccessfulDelivery(appointmentFhirId, messageType)` vóór publish.
2. **Publisher** — `AppointmentReminderPublisher` slaat over bij eerder succesvol verstuurd (`slaatOverBijEerderSuccesvolVerstuurd`).
3. **Consumer** — verwerkt alleen berichten met nog een `QUEUED`-record (`hasQueuedDeliveryRecord`).
4. **Scheduler-tick** — integratietest bewijst geen tweede publish bij herhaalde tick.

### Unit — publisher en delivery log

| Klasse | Tests | Relevante validatie |
|--------|-------|---------------------|
| `AppointmentReminderPublisherTest` | 5 | Dubbele publish voorkomen; 1u nog wel na succesvolle 24u |
| `NotificationDeliveryLogServiceTest` | 3 | `hasSuccessfulDelivery` na SENT; mislukte poging telt niet als succes |
| `AppointmentReminderEligibilityServiceTest` | 4 | Begonnen/geannuleerde afspraken niet opnieuw in aanmerking |

### Integratie

`AppointmentReminderSchedulingIntegrationTest.tweedeSchedulerTickQueueNietOpnieuwNaEerstePoging` — expliciete regressietest voor dubbele scheduler-runs.

**Lokaal aantonen:** [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) — na tweede scheduler-tick geen tweede `QUEUED` voor hetzelfde appointment; logmonitor toont vensterstatus en `sent`.

---

## 4. Appointment-poll (FHIR en JDBC)

Poll-logica staat in `OpenmrsFhirPollingService` + `AppointmentPollSource` (FHIR R5 of JDBC `patient_appointment`).

| Klasse | Tests | Validatie |
|--------|-------|-----------|
| `JpaAppointmentPollPersistenceTest` | 6 | Upsert, exclusies, organisatie-scope in DB |
| `OpenmrsPollOrganisationScopeTest` | 2 | Actieve bronnen per organisatie |
| `OpenmrsFhirPropertiesTest` | 5 | Multi-org intervals, `resolveActiveConnections` |
| `AppointmentFhirMapperTest` | 8 | FHIR → intern DTO |
| `PatientFhirMapperTest` | 2 | Patient/telefoon mapping |
| `OpenmrsFhirAppointmentMetadataTest` | 2 | OpenMRS-specifieke FHIR-metadata |
| `RetryingOpenmrsFhirOperationsTest` | 3 | Retry bij tijdelijke FHIR-fouten |

**Lokaal aantonen:** logmonitor — sectie *Laatste poll* (diagnostics), *Polled appointments*; bij JDBC-poll bron = MariaDB-URL, geen FHIR-metadata-call.

---

## 5. Herinneringstekst en annuleringen

| Klasse | Tests | Validatie |
|--------|-------|-----------|
| `DefaultAppointmentNotificationContentProviderTest` | 4 | Nederlandse body, lead-spec (24u/1u), instructies |
| `AppointmentReminderMessageBuilderTest` | 2 | Berichtpayload voor queue |
| `CancelledAppointmentNotificationServiceTest` | 4 | Geannuleerde afspraken: geen/on hold notificatie |
| `AppointmentWindowStatusResolverTest` | 1 | Vensterlabels in test-GUI |

---

## Overige automatische tests (ondersteunend)

| Domein | Voorbeelden | Tests |
|--------|-------------|-------|
| Scheduling-sync / tijdzone | `OpenmrsSchedulingSyncPropertiesTest`, `OpenmrsSchedulingStatusMapperTest`, `OpenmrsFhirResourceFactoryTest` | 8 |
| Config / security | `NotificationSchedulerPropertiesTest`, `OpenmrsFhirTlsApacheHttpClientTest`, `AesEncryptionServiceTest` | 12 |
| Organisatie-API | `OrganisationConfigControllerTest`, `OrganisationConfigServiceTest` | 5 |
| Context-load | `CommModuleApplicationTests` | 1 |

---

## Tests uitvoeren

```bash
./mvnw test
```

```powershell
.\mvnw.cmd test
```

Rapporten (Surefire): `target/surefire-reports/`.

---

## Lokaal demonstreren (checklist)

| # | Stap | Verwacht resultaat |
|---|------|-------------------|
| 1 | Distro + `docker compose up -d --build` | `comm-module-app` **healthy** |
| 2 | `curl http://localhost:8081/actuator/health` | `{"status":"UP"}` |
| 3 | POST `/api/notifications/test` | HTTP **202**, bericht op queue |
| 4 | Scheduling-test volgens [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) | Poll → `polled_appointment` → delivery log `QUEUED` → `SENT` |
| 5 | Tweede scheduler-tick | Geen dubbele herinnering (idempotentie) |

Logmonitor: http://localhost:8081/test-scheduling.html (status, poll-diagnostics, polled appointments met venster/`sent`, delivery log).

---

## Conclusie

De **scheduler**, **provider-adapters**, **idempotentie**, **poll** (FHIR/JDBC) en **herinneringstekst** zijn afgedekt met unit- en Spring-integratietests. De **141** automatische tests slagen; de **Docker-stack** (JDBC-poll op reference distro) en het scheduling-testplan maken de keten lokaal reproduceerbaar.
