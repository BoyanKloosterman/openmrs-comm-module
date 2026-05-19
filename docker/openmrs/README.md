# OpenMRS Docker (Legacy UI + afspraken)

## Modules in de image

| Module | Versie | Doel |
|--------|--------|------|
| serialization.xstream | 0.2.14 | Vereist voor reporting |
| htmlwidgets | 1.7.2 | Vereist voor reporting |
| calculation | 1.2 | Dependency reporting / appointmentscheduling |
| reporting | 1.28.0 | Vereist voor appointmentscheduling (≥ 0.9.2) |
| legacyui | 1.24.0 | Legacy UI — **niet** 2.0.0/2.1.0 (Maven vraagt platform 2.7.3) |
| webservices.rest | 2.36.0 | REST API (o.a. voor tooling) |
| appointmentscheduling | 2.0.0 | Afspraken plannen in Legacy UI |

Startvolgorde in de UI (als automatisch starten faalt): **serialization.xstream** → **htmlwidgets** → **calculation** → **reporting** → **legacyui** → **appointmentscheduling**.

### Scherm “no user interface module is installed”

De platform draait, maar **legacyui** is niet gestart. Vaak in de logs:

```text
Module requires version matching 2.7.3. Current code version is 2.5.14
```

Oorzaak: **legacyui** start niet (vaak `requires version matching 2.7.3` terwijl je op 2.5.14 zit). Zonder legacyui ontbreekt `AdministrationSectionExt` en zie je calculation/reporting-warnings.

Fix: image opnieuw bouwen (legacyui **1.24.0**), daarna **alleen** `openmrs_data` wissen en container recreate:

```powershell
docker compose build openmrs
docker compose down
docker volume rm openmrs-comm-module_openmrs_data
docker compose up -d openmrs
```

## Image bouwen en container vervangen

```powershell
docker compose build openmrs
docker compose up -d --force-recreate openmrs
```

Eerste start na nieuwe modules kan enkele minuten duren (module-install + DB-update).

### Platform wisselen (2.7 → 2.5): verse database verplicht

OpenMRS **downgraden** op dezelfde Postgres-database faalt (Liquibase), bijvoorbeeld:

```text
ERROR: function "uuid" already exists ... CREATE FUNCTION UUID() ...
```

De DB is dan nog van **2.7**, terwijl **2.5.14** oudere changelogs opnieuw wil draaien. Oplossing in de testomgeving: **beide** volumes wissen (OpenMRS + Postgres; comm-module gebruikt dezelfde DB):

```powershell
docker compose down
docker volume rm openmrs-comm-module_postgres_data openmrs-comm-module_openmrs_data
docker compose up -d --build
```

Controleer volumenamen met `docker volume ls` als de rm faalt.

Na een verse start: admin-wachtwoord opnieuw uit `.env` (`OMRS_ADMIN_USER_PASSWORD`), modules starten automatisch of handmatig (zie startvolgorde hierboven).

**Bestaande volume (zelfde platform):** verwijder oude `serialization.xstream` **0.2.15** (Manage Modules → Unload), daarna `docker compose build openmrs` en recreate. Alleen `openmrs_data` wissen is dan genoeg:

```powershell
docker compose down
docker volume rm openmrs-comm-module_openmrs_data
docker compose up -d --build openmrs
```

## Afspraken maken in Legacy UI

1. Login: [http://localhost:8080/openmrs](http://localhost:8080/openmrs) (`admin` + wachtwoord uit `.env`).
2. Controleer **Administration → Manage Modules**: `Appointment Scheduling Module` = **Started**.
3. Menu **Appointments** (bovenaan) of op het **patient dashboard** tab **Appointments**.

**URLs (na inloggen):**

| Actie | URL |
|--------|-----|
| Lijst / beheer | `/openmrs/module/appointmentscheduling/appointmentList.list` |
| Nieuwe afspraak | `/openmrs/module/appointmentscheduling/appointmentForm.form` |

Op **OpenMRS 2.7** krijg je op die URLs 404 (`DispatcherServlet.noHandlerFound`); daarom draait deze image op **2.5.14**.

4. Eerste keer: **Administration → Manage Appointment Scheduling Module** — stel o.a. telefoon person attribute in.

### Demo-rooster (tijdsloten) seeden

OpenMRS toont alleen tijden waarvoor **provider schedule** (tijdsloten) bestaat. Een geboekte afspraak **vult** dat uur; **Find Available Times** slaat volle slots over. De lijst **Appointments** toont alleen echte boekingen, niet “vrije uren”.

```powershell
cd C:\Users\kloos\Documents\Apps\openmrs-comm-module
.\docker\openmrs\seed-appointments.ps1
```

Standaard: **demo-provider** (Super User), type **Consult**, **90 dagen** vanaf vandaag, elk uur **08:00–18:00 UTC**. Optioneel: `-DaysAhead 180 -StartHour 8 -EndHour 18`.

Na nieuwe boekingen hoef je het script niet opnieuw te draaien; wel opnieuw na een **verse database** of als je verder in de toekomst wilt plannen (run het script dan nog eens).

### Formulier: juiste volgorde (anders leeg save-venster)

| Stap | Actie |
|------|--------|
| 1 | Patient, **Appointment type**, optioneel locatie/provider |
| 2 | **Between**: vandaag t/m over ~90 dagen, **overdag** (bijv. 08:00 AM – 06:00 PM) |
| 3 | **Find Available Times** — er moeten rijen in de tabel verschijnen |
| 4 | **Klik één rij** (radio/selectie) — provider/locatie/tijd komen uit die rij |
| 5 | **Save Appointment** → bevestiging moet gevuld zijn → **Save** in het venster |

**Find Time lijkt “niets te doen”** als er geen slot in je datumbereik valt (bijv. zoeken 21–22 mei terwijl slots alleen op 20 mei staan). Het save-venster toont dan lege provider/locatie/datum; de grijze **Save**-knop komt doordat je geen tijdslot hebt geselecteerd.

**DB controleren** (vanuit projectmap):

```powershell
docker compose exec postgres psql -U openmrs_user -d openmrs -c "SELECT COUNT(*) FROM appointmentscheduling_time_slot;"
docker compose exec postgres psql -U openmrs_user -d openmrs -c "SELECT COUNT(*) FROM appointmentscheduling_appointment;"
```

### Datumfilter: `Unparseable date: "05/19/2026 00:00"`

De appointment-pagina’s gebruiken **US-datum** (`mm/dd/yyyy`) in JavaScript, maar bij locale **`en`** vult het formulier de tijd als **`00:00`** (24 uur). De server verwacht dan **`12:00 AM`** — parsing faalt.

**Oplossing (testomgeving):** locale **`en_US`** voor admin + default:

```powershell
docker compose exec postgres psql -U openmrs_user -d openmrs -c "UPDATE global_property SET property_value = 'en_US' WHERE property = 'default_locale'; UPDATE user_property SET property_value = 'en_US' WHERE user_id = 1 AND property = 'defaultLocale';"
```

Daarna **uitloggen en opnieuw inloggen**. Datums in filters dan bijv. `05/19/2026 12:00 AM` t/m `05/19/2026 11:59 PM` (via kalender-icoon, niet handmatig `00:00` typen).

In de UI: **My Profile → Default Locale → English (United States)**.

## Koppeling met comm-module (belangrijk)

Keten na boeken in Legacy UI:

1. **OpenMRS→FHIR sync** (comm-module, elke minuut): `appointmentscheduling_*` → HAPI FHIR (`omrs-appt-{id}`)
2. **FHIR poll** → `polled_appointment`
3. **24u-scheduler** → RabbitMQ-queue (bij `COMM_NOTIFICATION_REMINDER_LEAD_HOURS=24`)

Instellingen in `.env`: `OPENMRS_SCHEDULING_FHIR_SYNC_ENABLED=true`, zone `OPENMRS_SCHEDULING_SYNC_ZONE=Europe/Amsterdam` (08:00 UI = lokale tijd). Zonder telefoon op de patiënt: `OPENMRS_SCHEDULING_SYNC_FALLBACK_PHONE` (alleen test).

Zie `docs/docker-scheduling-test.md` voor de FHIR-testketen.
