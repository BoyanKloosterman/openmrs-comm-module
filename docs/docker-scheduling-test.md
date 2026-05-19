# Stappenplan: scheduling testen via Docker

Test van de volledige keten: **FHIR-poll** → `polled_encounter` → **notificatie-scheduler** → RabbitMQ → provider (fakecomworld) → `notification_delivery_log`.

## Vereisten

- Docker en Docker Compose
- Projectmap met werkende `.env` (kopie van `.env.example`)
- Poorten vrij: **8081** (comm-module), **8080** (OpenMRS), **15672** (RabbitMQ UI)

---

## Stap 1 — `.env` instellen

Kopieer en pas aan:

```powershell
cp .env.example .env
```

Minimaal controleren:

| Variabele | Doel |
|-----------|------|
| `SPRING_DATASOURCE_*` / `SPRING_RABBITMQ_*` | Zelfde waarden als Postgres/RabbitMQ in compose |
| `OPENMRS_FHIR_*` | FHIR naar OpenMRS in Docker |
| `OPENMRS_FHIR_ORGANISATION_ID` | Moet overeenkomen met `polled_encounter.organisation_id` (default: `default`) |
| `APP_ENCRYPTION_KEY` | Exact **32 tekens**;zelfde key bij elke compose-run (anders decrypt-fouten) |

### Snelle scheduling-test (aanbevolen)

Zet in `.env` (tijdelijk, niet productie):

```env
COMM_NOTIFICATION_REMINDER_LEAD_HOURS=1
COMM_NOTIFICATION_REMINDER_WINDOW_MINUTES=120
COMM_NOTIFICATION_SCHEDULER_CHECK_INTERVAL_MINUTES=1
OPENMRS_FHIR_POLL_INTERVAL_MINUTES=1
```

- Venster: afspraak tussen **nu** en **nu + 2 uur** (UTC), rond **nu + 1 uur**.
- Scheduler en FHIR-poll draaien elke minuut.

### Productie-achtige test (24 uur)

```env
COMM_NOTIFICATION_REMINDER_LEAD_HOURS=24
COMM_NOTIFICATION_REMINDER_WINDOW_MINUTES=60
```

Afspraak moet dan op **nu + 24 uur ± 30 min** (UTC) liggen.

> **Let op:** deze variabelen worden alleen in de container gezet als ze in `docker-compose.yml` onder `comm-module.environment` staan (is zo geconfigureerd). Na wijziging in `.env`: container opnieuw aanmaken (stap 2).

---

## Stap 2 — Stack starten

In de projectmap:

```powershell
docker compose up -d --build
```

Na wijziging in `.env` of `docker-compose.yml`:

```powershell
docker compose up -d --force-recreate comm-module
```

Status:

```powershell
docker compose ps
```

Wacht tot `comm-module-app` en `comm-module-db` healthy zijn. OpenMRS kan enkele minuten opstarten; de comm-module start al en retried FHIR.

Health comm-module:

```text
http://localhost:8081/actuator/health
```

---

## Stap 3 — Testdata klaarzetten

Kies **één** van de twee methoden.

### Methode A — Handmatig in PostgreSQL (snelst)

1. Open psql in de DB-container:

```powershell
docker exec -it comm-module-db psql -U openmrs_user -d openmrs
```

2. **Snelle test (lead = 1 uur):** zet encounter op nu + 1u5m in UTC:

```sql
INSERT INTO polled_encounter (
  organisation_id,
  encounter_uuid,
  encounter_fhir_id,
  patient_fhir_id,
  encounter_datetime,
  voided,
  last_polled_at,
  patient_phone,
  patient_display_name
)
SELECT
  'default',
  'uuid-test-sched-001',
  'enc-test-sched-001',
  'pat-test-sched-001',
  (NOW() AT TIME ZONE 'UTC' + INTERVAL '1 hour 5 minutes'),
  false,
  NOW() AT TIME ZONE 'UTC',
  patient_phone,
  'Test Patiënt'
FROM polled_encounter
WHERE patient_phone IS NOT NULL
LIMIT 1;
```

Heb je nog geen rij met versleutelde telefoon: eerst één encounter via OpenMRS + FHIR-poll (methode B), of kopieer `patient_phone` van een bestaande rij.

**24u-test:** vervang interval door `'24 hours 5 minutes'`.

3. Controleer of Java de rij zou vinden (UTC). Voor lead **1** uur en window **120** min:

```sql
WITH b AS (
  SELECT
    (NOW() AT TIME ZONE 'UTC') AS now_utc,
    (NOW() AT TIME ZONE 'UTC' + INTERVAL '30 minutes') AS window_start_utc,
    (NOW() AT TIME ZONE 'UTC' + INTERVAL '2 hours 30 minutes') AS window_end_utc
)
SELECT
  e.encounter_fhir_id,
  e.encounter_datetime AT TIME ZONE 'UTC' AS encounter_utc,
  b.window_start_utc,
  b.window_end_utc,
  CASE
    WHEN e.organisation_id <> 'default' THEN 'verkeerde org'
    WHEN e.voided THEN 'voided'
    WHEN e.encounter_datetime AT TIME ZONE 'UTC' <= b.now_utc THEN 'al verstreken'
    WHEN e.encounter_datetime AT TIME ZONE 'UTC' >= b.window_start_utc
     AND e.encounter_datetime AT TIME ZONE 'UTC' <  b.window_end_utc
    THEN 'OK voor scheduler'
    ELSE 'buiten venster'
  END AS status
FROM polled_encounter e, b b
WHERE e.encounter_fhir_id = 'enc-test-sched-001';
```

Voor **lead 24** / window **60**: vervang `30 minutes` door `23 hours 30 minutes` en `2 hours 30 minutes` door `24 hours 30 minutes`.

**Bestaande rij bijwerken:**

```sql
UPDATE polled_encounter
SET encounter_datetime = (NOW() AT TIME ZONE 'UTC' + INTERVAL '1 hour 5 minutes'),
    voided = false,
    organisation_id = 'default',
    last_polled_at = NOW() AT TIME ZONE 'UTC'
WHERE encounter_fhir_id = 'enc-test-docker-002';
```

> Gebruik altijd `NOW() AT TIME ZONE 'UTC'`. Tijden alleen in +02 in de UI kunnen **buiten venster** lijken terwijl SQL “ok” toont.

### Methode B — Via OpenMRS (realistischer)

1. Open [http://localhost:8080/openmrs](http://localhost:8080/openmrs) — login `admin` / wachtwoord uit `.env` (`OPENMRS_FHIR_PASSWORD` of `OMRS_ADMIN_USER_PASSWORD`).
2. Maak een **patiënt** met telefoonnummer (FHIR telecom type phone).
3. Maak een **encounter/visit** met starttijd in het juiste venster (bij lead=1: over ~1 uur).
4. Wacht max. **2 minuten** op FHIR-poll (`Encounter-poll:` in logs).
5. Controleer DB:

```sql
SELECT encounter_fhir_id, encounter_datetime, patient_phone IS NOT NULL AS heeft_telefoon
FROM polled_encounter
WHERE organisation_id = 'default'
ORDER BY last_polled_at DESC
LIMIT 5;
```

Zo nodig alleen `encounter_datetime` bijstellen met de UPDATE hierboven (UTC).

---

## Stap 4 — Scheduler laten lopen en logs volgen

```powershell
docker compose logs -f comm-module
```

Verwacht binnen ~1 minuut (bij `CHECK_INTERVAL_MINUTES=1`):

```text
24u-herinnering: 1 in venster, 1 op queue gezet
24u-herinnering in queue: notificationId=... encounter=... naar +31...
Verzendstatus QUEUED: ...
Verzendstatus SENT: ...
```

Bij `0 in venster`: zie stap 5 (fouten).

---

## Stap 5 — Resultaat controleren

### Delivery log (PostgreSQL)

```powershell
docker exec comm-module-db psql -U openmrs_user -d openmrs -c "SELECT encounter_fhir_id, message_type, status, provider, attempted_at FROM notification_delivery_log ORDER BY attempted_at DESC LIMIT 5;"
```

Verwacht: `message_type = APPOINTMENT_REMINDER_24H`, eerst `QUEUED`, daarna `SENT`.

### RabbitMQ

- UI: [http://localhost:15672](http://localhost:15672) — user/wachtwoord uit `.env` (`RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS`)
- Queue o.a. `queue.swiftsend` (default provider SWIFTSEND)

### Tweede scheduler-tick (idempotentie)

Wacht nog 1 minuut. Logs moeten blijven: `0 in venster` of geen tweede queue voor hetzelfde encounter (al gelogd).

---

## Stap 6 — Optioneel: alleen queue testen (geen scheduling)

Dit test **niet** de scheduler, alleen RabbitMQ + consumer:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/notifications/test
```

---

## Veelvoorkomende problemen

| Symptoom | Oorzaak | Oplossing |
|----------|---------|-----------|
| `0 in venster` | `encounter_datetime` buiten venster (UTC) | UPDATE met `NOW() AT TIME ZONE 'UTC' + interval` |
| SQL “IN VENSTER”, app niet | Timezone: +02 vs UTC | Altijd UTC-query gebruiken (stap 3) |
| `.env` wijziging werkt niet | Container niet opnieuw gemaakt | `docker compose up -d --force-recreate comm-module` |
| `Could not decrypt` | `APP_ENCRYPTION_KEY` gewijzigd of platte tekst in DB | Zelfde key als bij insert; telefoon van FHIR-rij kopiëren |
| Geen telefoon | `patient_phone` leeg | Patiënt in OpenMRS of gekopieerde encrypted kolom |
| `verkeerde org` | `organisation_id` ≠ `OPENMRS_FHIR_ORGANISATION_ID` | Beide op `default` zetten |
| Geen FHIR-data | OpenMRS nog niet klaar | Logs `FHIR poll mislukt`; wachten / credentials checken |

---

## Opnieuw testen

Verwijder log voor hetzelfde encounter (alleen testomgeving):

```sql
DELETE FROM notification_delivery_log WHERE encounter_fhir_id = 'enc-test-sched-001';
```

Zet `encounter_datetime` opnieuw in het venster en wacht op de volgende scheduler-tick.

---

## Stack stoppen

```powershell
docker compose down
```

Met verwijderen van volumes (alle DB-data weg):

```powershell
docker compose down -v
```

---

## Geautomatiseerde tests (zonder Docker)

Zelfde logica als unit/integratietests:

```powershell
.\mvnw.cmd test -Dtest=AppointmentReminderSchedulingIntegrationTest,NotificationSchedulerTest
```

Zie `src/test/java/.../AppointmentReminderSchedulingIntegrationTest.java`.
