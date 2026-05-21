# Testrapportage — OpenMRS Communicatiemodule

Versie 1.0 | Sprint 3 | Meting: 21 mei 2026

Deze rapportage beschrijft hoe de **kernlogica** van de module is afgedekt met geautomatiseerde tests en hoe de **werking lokaal** kan worden aangetoond. Het sluit aan op deliverable 6 uit [docs/sprint3-doelen.txt](docs/sprint3-doelen.txt).

---

## Samenvatting

| Indicator | Resultaat |
|-----------|-----------|
| Totaal aantal tests | **106** |
| Geslaagd / mislukt / overgeslagen | **106 / 0 / 0** |
| Build | `mvnw test` — **BUILD SUCCESS** (~20 s) |
| Integratietests (Spring Boot + H2/Postgres-testcontext) | **13** tests in 5 klassen |
| Provider-adapter unit-tests | **20** tests (4 providers) |

---

## Teststrategie

| Laag | Doel | Techniek |
|------|------|----------|
| **Unit** | Geïsoleerde businesslogica (scheduler, publisher, providers, consumer) | JUnit 5 + Mockito |
| **Integratie** | Scheduler + delivery log + DB + publisher in één Spring-context | `@SpringBootTest`, vaste `Clock`, gemockte FHIR/RabbitMQ waar nodig |
| **Lokaal handmatig** | End-to-end keten in Docker (OpenMRS → FHIR → poll → scheduler → queue → fake provider) | Compose-stack, test-GUI, [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) |

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

| Klasse | Test | Validatie |
|--------|------|-----------|
| `AppointmentReminderSchedulingIntegrationTest` | `processorZetAppointmentIn24uVensterOpQueue` | Afspraak in venster → één RabbitMQ-publish + `QUEUED` in delivery log |
| | `tweedeSchedulerTickQueueNietOpnieuwNaEerstePoging` | **Idempotentie:** twee scheduler-ticks → nog steeds **één** publish |
| | `schedulerTickMetUitgeschakeldeSchedulerQueueNiets` | Scheduler uit → geen publish |
| `AppointmentReminder1HourSchedulingIntegrationTest` | 1u-herinnering in venster | Aparte lead (1 uur) werkt naast 24u |
| `AppointmentCancellationIntegrationTest` | Geannuleerde afspraak | Geen herinnering na void/cancel |

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

| Klasse | Relevante tests |
|--------|-----------------|
| `AppointmentReminderPublisherTest` | Dubbele publish voorkomen; 1u nog wel na succesvolle 24u |
| `NotificationDeliveryLogServiceTest` | `hasSuccessfulDelivery` na SENT; mislukte poging telt niet als succes |
| `AppointmentReminderEligibilityServiceTest` | Begonnen/geannuleerde afspraken niet opnieuw in aanmerking |

### Integratie

`AppointmentReminderSchedulingIntegrationTest.tweedeSchedulerTickQueueNietOpnieuwNaEerstePoging` — expliciete regressietest voor dubbele scheduler-runs.

**Lokaal aantonen:** [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) — sectie *Tweede scheduler-tick (idempotentie)*: na tweede minuut `div class="0 in venster"` of geen tweede `QUEUED` voor hetzelfde appointment.

---

## Overige automatische tests (ondersteunend)

| Domein | Voorbeelden | Tests |
|--------|-------------|-------|
| FHIR-mapping / poll | `AppointmentFhirMapperTest`, `JpaAppointmentPollPersistenceTest`, `RetryingOpenmrsFhirOperationsTest` | 16 |
| Berichtopbouw / query | `AppointmentReminderMessageBuilderTest`, `AppointmentReminderQueryServiceTest` | 14 |
| Config / security | `NotificationSchedulerPropertiesTest`, `AesEncryptionServiceTest`, `OpenmrsFhirTlsApacheHttpClientTest` | 11 |
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
| 1 | `docker compose up -d --build` | `comm-module-app` **healthy** |
| 2 | `curl http://localhost:8081/actuator/health` | `{"status":"UP"}` |
| 3 | POST `/api/notifications/test` | HTTP **202**, bericht op queue |
| 4 | Scheduling-test volgens [docs/docker-scheduling-test.md](docs/docker-scheduling-test.md) | Delivery log: `QUEUED` → `SENT` |
| 5 | Tweede scheduler-tick | Geen dubbele herinnering (idempotentie) |

Optionele GUI: http://localhost:8081/test-scheduling.html

---

## Conclusie

De **scheduler**, **provider-adapters** en **idempotentie** zijn afgedekt met gerichte unit-tests en Spring-integratietests met vaste tijd (`Clock`) en gecontroleerde data. De **106** automatische tests slagen; de **Docker-stack** en het scheduling-testplan maken de keten lokaal reproduceerbaar voor beoordeling en beheerdersvalidatie.
