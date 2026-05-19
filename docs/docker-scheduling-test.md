# Stappenplan: scheduling testen via Docker

Test van de volledige keten: **FHIR-poll** → `polled_appointment` → **notificatie-scheduler** → RabbitMQ → provider (fakecomworld) → `notification_delivery_log`.

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
| `OPENMRS_FHIR_*` | FHIR R5-server (Docker: `http://fhir-r5:8080/fhir`, host: poort **8082**) |
| `OPENMRS_FHIR_ORGANISATION_ID` | Moet overeenkomen met `polled_appointment.organisation_id` (default: `default`) |
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

2. **Snelle test (lead = 1 uur):** zet appointment op nu + 1u5m in UTC:

```sql
INSERT INTO polled_appointment (
  organisation_id,
  appointment_uuid,
  appointment_fhir_id,
  patient_fhir_id,
  appointment_datetime,
  voided,
  last_polled_at,
  patient_phone,
  patient_display_name
)
SELECT
  'default',
  'uuid-test-sched-001',
  'apt-test-sched-001',
  'pat-test-sched-001',
  (NOW() AT TIME ZONE 'UTC' + INTERVAL '1 hour 5 minutes'),
  false,
  NOW() AT TIME ZONE 'UTC',
  patient_phone,
  'Test Patiënt'
FROM polled_appointment
WHERE patient_phone IS NOT NULL
LIMIT 1;
```

Heb je nog geen rij met versleutelde telefoon: eerst één appointment via OpenMRS + FHIR-poll (methode B), of kopieer `patient_phone` van een bestaande rij.

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
  a.appointment_fhir_id,
  a.appointment_datetime AT TIME ZONE 'UTC' AS appointment_utc,
  b.window_start_utc,
  b.window_end_utc,
  CASE
    WHEN a.organisation_id <> 'default' THEN 'verkeerde org'
    WHEN a.voided THEN 'voided'
    WHEN a.appointment_datetime AT TIME ZONE 'UTC' <= b.now_utc THEN 'al verstreken'
    WHEN a.appointment_datetime AT TIME ZONE 'UTC' >= b.window_start_utc
     AND a.appointment_datetime AT TIME ZONE 'UTC' <  b.window_end_utc
    THEN 'OK voor scheduler'
    ELSE 'buiten venster'
  END AS status
FROM polled_appointment a, b b
WHERE a.appointment_fhir_id = 'apt-test-sched-001';
```

Voor **lead 24** / window **60**: vervang `30 minutes` door `23 hours 30 minutes` en `2 hours 30 minutes` door `24 hours 30 minutes`.

**Bestaande rij bijwerken:**

```sql
UPDATE polled_appointment
SET appointment_datetime = (NOW() AT TIME ZONE 'UTC' + INTERVAL '1 hour 5 minutes'),
    voided = false,
    organisation_id = 'default',
    last_polled_at = NOW() AT TIME ZONE 'UTC'
WHERE appointment_fhir_id = 'apt-test-docker-002';
```

> Gebruik altijd `NOW() AT TIME ZONE 'UTC'`. Tijden alleen in +02 in de UI kunnen **buiten venster** lijken terwijl SQL “ok” toont.

### Methode B — Via OpenMRS (realistischer)

1. Open [http://localhost:8080/openmrs](http://localhost:8080/openmrs) — login `admin` / wachtwoord uit `.env` (`OPENMRS_FHIR_PASSWORD` of `OMRS_ADMIN_USER_PASSWORD`).
2. Maak een **patiënt** met telefoonnummer (FHIR telecom type phone).
3. Maak een **appointment** als FHIR R5-resource op de HAPI-server (`PUT http://localhost:8082/fhir/Appointment/{id}` of via UI op poort 8082). Zie [HL7 Appointment](https://www.hl7.org/fhir/appointment.html). Bij een verse `docker compose up` vult `fhir-r5-seed` al een test-Patient en -Appointment.
4. Wacht max. **2 minuten** op de poll (`Appointment-poll:` in logs).
5. Controleer DB:

```sql
SELECT appointment_fhir_id, appointment_datetime, patient_phone IS NOT NULL AS heeft_telefoon
FROM polled_appointment
WHERE organisation_id = 'default'
ORDER BY last_polled_at DESC
LIMIT 5;
```

Zo nodig alleen `appointment_datetime` bijstellen met de UPDATE hierboven (UTC).

---

## Stap 4 — Scheduler laten lopen en logs volgen

```powershell
docker compose logs -f comm-module
```

Verwacht binnen ~1 minuut (bij `CHECK_INTERVAL_MINUTES=1`):

```text
24u-herinnering: 1 in venster, 1 op queue gezet
24u-herinnering in queue: notificationId=... appointment=... naar +31...
Verzendstatus QUEUED: ...
Verzendstatus SENT: ...
```

Bij `0 in venster`: zie stap 5 (fouten).

---

## Stap 5 — Resultaat controleren

### Delivery log (PostgreSQL)

```powershell
docker exec comm-module-db psql -U openmrs_user -d openmrs -c "SELECT appointment_fhir_id, message_type, status, provider, attempted_at FROM notification_delivery_log ORDER BY attempted_at DESC LIMIT 5;"
```

Verwacht: `message_type = APPOINTMENT_REMINDER_24H`, eerst `QUEUED`, daarna `SENT`.

### RabbitMQ

- UI: [http://localhost:15672](http://localhost:15672) — user/wachtwoord uit `.env` (`RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS`)
- Queue o.a. `queue.swiftsend` (default provider SWIFTSEND)

### Tweede scheduler-tick (idempotentie)

Wacht nog 1 minuut. Logs moeten blijven: `0 in venster` of geen tweede queue voor hetzelfde appointment (al gelogd).

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
| `0 in venster` | `appointment_datetime` buiten venster (UTC) | UPDATE met `NOW() AT TIME ZONE 'UTC' + interval` |
| SQL “IN VENSTER”, app niet | Timezone: +02 vs UTC | Altijd UTC-query gebruiken (stap 3) |
| `.env` wijziging werkt niet | Container niet opnieuw gemaakt | `docker compose up -d --force-recreate comm-module` |
| `Could not decrypt` | `APP_ENCRYPTION_KEY` gewijzigd of platte tekst in DB | Zelfde key als bij insert; telefoon van FHIR-rij kopiëren |
| Geen telefoon | `patient_phone` leeg | Patiënt in OpenMRS of gekopieerde encrypted kolom |
| `verkeerde org` | `organisation_id` ≠ `OPENMRS_FHIR_ORGANISATION_ID` | Beide op `default` zetten |
| Geen Appointment-data | FHIR R5-server nog niet healthy / verkeerde URL | Logs `OpenMRS FHIR poll mislukt`; `OPENMRS_FHIR_SERVER_URL` moet `http://fhir-r5:8080/fhir` zijn (niet OpenMRS `/ws/fhir2/R5`). |
| `dependency fhir-r5 failed to start` | Oude healthcheck of `latest-tomcat` (rechten op `/webapps/ROOT`) | Gebruik `hapiproject/hapi:latest` + `docker compose up -d --force-recreate fhir-r5`. Eerste start duurt 5–8 min. |
| `Unable to create the directory .../webapps/ROOT` | Verkeerde image `latest-tomcat` | In compose staat `hapiproject/hapi:latest` (Spring Boot). |
| 404 op FHIR metadata | Verkeerde base-URL of HAPI nog aan het opstarten | `curl http://localhost:8082/fhir/metadata` — `fhirVersion` moet **5.0.0** zijn. |

---

## Opnieuw testen

Verwijder log voor hetzelfde appointment (alleen testomgeving):

```sql
DELETE FROM notification_delivery_log WHERE appointment_fhir_id = 'apt-test-sched-001';
```

Zet `appointment_datetime` opnieuw in het venster en wacht op de volgende scheduler-tick.

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
