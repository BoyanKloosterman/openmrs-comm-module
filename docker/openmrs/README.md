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

## Koppeling met comm-module (belangrijk)

De **comm-module pollt FHIR R5** (`fhir-r5` / poort 8082), niet automatisch OpenMRS Appointment Scheduling.

- Afspraken in OpenMRS → alleen in OpenMRS DB.
- Afspraken voor polling/herinneringen → `Patient` + `Appointment` op HAPI (seed of handmatig PUT), of later FHIR2/sync.

Zie `docs/docker-scheduling-test.md` voor de FHIR-testketen.
