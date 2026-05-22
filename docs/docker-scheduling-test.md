# Stappenplan: scheduling testen met externe OpenMRS distro

Test van de keten: **afspraak in distro** → **FHIR2 R5 poll** → `polled_appointment` → **notificatie-scheduler** → RabbitMQ → fakecomworld → `notification_delivery_log`.

## Vereisten

- Docker en Docker Compose
- [openmrs-distro-referenceapplication](https://github.com/openmrs/openmrs-distro-referenceapplication) draait (`docker compose up` op poort **80**)
- Projectmap comm-module met `.env` (kopie van `.env.example`)
- Poorten vrij: **8081** (comm-module), **80** (distro)

---

## Stap 1 — `.env` instellen

```powershell
cp .env.example .env
```

| Variabele | Doel |
|-----------|------|
| `OPENMRS_DATASOURCE_URL` | `jdbc:mariadb://host.docker.internal:3307/openmrs` |
| `OPENMRS_DATASOURCE_USERNAME` / `PASSWORD` | `openmrs` / `openmrs` (distro `.env`) |
| `OPENMRS_SCHEDULING_SOURCE` | `patient-appointment` (SPA-tabel, geen legacy scheduling) |
| `OPENMRS_FHIR_POLL_MODE` | `jdbc` (distro FHIR2 heeft geen Appointment-resource) |
| `OPENMRS_SCHEDULING_FHIR_SYNC_ENABLED` | `false` |
| `APP_ENCRYPTION_KEY` | Exact **32 tekens**, stabiel tussen runs |

### Snelle scheduling-test (1u-venster)

```env
COMM_NOTIFICATION_REMINDER_1_LEAD_HOURS=1
COMM_NOTIFICATION_REMINDER_WINDOW_MINUTES=120
COMM_NOTIFICATION_SCHEDULER_CHECK_INTERVAL_MINUTES=1
OPENMRS_FHIR_POLL_INTERVAL_MINUTES=1
```

Boek in de distro een afspraak op **nu + 1 uur ± 30 min** (UTC), met patiënt met telefoonnummer.

---

## Stap 2 — Stacks starten

**Distro** (aparte map):

```powershell
cd ..\openmrs-distro-referenceapplication
docker compose up -d
```

**Comm-module**:

```powershell
cd openmrs-comm-module
docker compose up -d --build
```

Na `.env`-wijziging:

```powershell
docker compose up -d --force-recreate comm-module
```

Health: http://localhost:8081/actuator/health

---

## Stap 3 — Afspraak in OpenMRS distro

1. Open http://localhost/openmrs/spa/home/appointments  
2. Log in (`admin` / `Admin123` tenzij gewijzigd)  
3. Maak een afspraak voor een patiënt met **telefoon**-attribuut  

Optioneel: controleer FHIR:

```powershell
curl -sS -u admin:Admin123 "http://localhost/openmrs/ws/fhir2/R5/Appointment?_count=5"
```

---

## Stap 4 — Logmonitor

Open http://localhost:8081/test-scheduling.html

| Sectie | Wat u ziet |
|--------|------------|
| Status & configuratie | FHIR-URL, poll-venster, scheduler, herinneringsvensters |
| Laatste FHIR-poll | Ruw/gemapt/opgeslagen/overgeslagen + eventuele fout |
| Polled appointments | DB-rijen met vensterstatus en telefoon |
| Delivery log | Verwerkte herinneringen (OK / fout) |

Klik **FHIR poll nu** na het boeken. Toast toont poll-samenvatting (niet alleen "voltooid"). Auto-refresh elke 10 seconden.

---

## Stap 5 — Scheduler / logs

Achtergrond-scheduler draait als `COMM_NOTIFICATION_SCHEDULER_ENABLED=true` (default).

Logs comm-module:

```powershell
docker compose logs -f comm-module
```

Zoek naar `OpenMRS FHIR poll`, `notification`, `delivery`.

---

## Problemen

| Symptoom | Oorzaak | Actie |
|----------|---------|--------|
| Geen polled appointments | FHIR-URL/auth of geen Appointment in FHIR2 | `curl` metadata/Appointment; check `.env` en distro-modules |
| `Connection refused` host.docker.internal | Distro niet op host:80 | Start distro; op Linux: `extra_hosts` staat in compose |
| Geen delivery log | Geen telefoon, buiten venster, scheduler uit | Patiënt met tel.; pas lead/window aan; check logs |
| Oude Postgres-data | Vorige stack met andere DB-naam | `docker compose down -v` of pas `POSTGRES_DB` aan |

---

## API (optioneel)

```bash
curl -sS http://localhost:8081/api/test/scheduling/status
curl -sS http://localhost:8081/api/test/scheduling/poll-diagnostics
curl -sS -X POST http://localhost:8081/api/test/scheduling/poll
curl -sS http://localhost:8081/api/test/scheduling/delivery-logs
```

Volledige test-API (boeken/scheduler) blijft beschikbaar onder `/api/test/scheduling/*`; de HTML-logmonitor gebruikt alleen status, poll, appointments en delivery-logs.
