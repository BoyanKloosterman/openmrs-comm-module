# Sprint 1 Deliverables — OpenMRS Communicatiemodule

Versie 1.0 | Groepsproject HBO Software Engineering

---

## 1. OpenMRS-omgeving en integratiepunten

OpenMRS is een open-source patiëntendossier systeem. Ziekenhuizen en klinieken wereldwijd gebruiken het om patiëntgegevens en afspraken bij te houden. De communicatiemodule staat buiten OpenMRS maar heeft er data van nodig.

### Integratiepunten

| Punt | Wat het is |
|------|-----------|
| FHIR REST API | De standaard API van OpenMRS 2.7+. Hieruit halen we afspraken op (Appointment-resource) en patiëntgegevens (Patient-resource) via een gewone HTTP GET. |
| Atom Feed | Een ingebouwde event-feed in OpenMRS. Overwogen maar niet gekozen (zie ADR 3). |
| Webhook (push) | OpenMRS kan events naar onze module pushen. Overwogen, niet gekozen vanwege risico bij downtime. |
| Authenticatie | Communicatie met OpenMRS gaat via Basic Auth of een token. Per organisatie opgeslagen in de module. |
| Messaging providers | SwiftSend, LegacyLink, AsyncFlow en SecurePost. Elk met hun eigen API. |

---

## 2. ADR 1 — Zelfstandige module of ingebouwde module?

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 22-4-2026 |
| Besloten door | Hele projectgroep |

### Opties

**Optie A: ingebouwde OpenMRS-module**
Een .omod-bestand (plugin) die in hetzelfde systeem als OpenMRS draait met directe databasetoegang.

Nadelen:
- Elk ziekenhuis installeert zijn eigen versie — centraal updaten of monitoren is onmogelijk.
- Schalen naar meerdere ziekenhuizen werkt niet.
- Afhankelijk van de Java-versie en het OpenMRS-platform.
- SaaS-model van de opdrachtgever is zo onmogelijk.

**Optie B: zelfstandige SaaS-module (onze keuze)**
De communicatiemodule draait als eigen applicatie, los van OpenMRS. Communiceert via de FHIR REST API.

Voordelen:
- Past bij het SaaS-model van de opdrachtgever.
- Centraal updaten, monitoren en schalen zonder dat de klant er iets van merkt.
- Ziekenhuizen koppelen door eenmalig hun URL en inloggegevens in te vullen.

### Beslissing
**Zelfstandige SaaS-module.**

### Gevolgen
- De module heeft zijn eigen Docker-container en eigen database.
- Communicatie met OpenMRS gaat altijd via de FHIR REST API.

---

## 3. ADR 2 — Welke technologie gebruiken we?

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 22-4-2026 |
| Besloten door | Hele projectgroep |

### 3.1 Programmeertaal

| Criterium | G | Java 17 | Python (FastAPI) | Node.js |
|-----------|---|---------|-----------------|---------|
| Leergemak voor het team | 5 | 4/5 | 4/5 | 3/5 |
| FHIR/HL7-bibliotheekondersteuning | 5 | 5/5 | 3/5 | 2/5 |
| Geschiktheid voor backend | 4 | 5/5 | 3/5 | 3/5 |
| Testbaarheid | 3 | 5/5 | 4/5 | 3/5 |
| Documentatie en community | 3 | 5/5 | 4/5 | 4/5 |
| Performance | 2 | 4/5 | 3/5 | 3/5 |
| **Totaalscore** | | **103/110** | 77/110 | 64/110 |

**Keuze: Java 17.** Doorslag: HAPI FHIR is de beste FHIR-bibliotheek en is in Java geschreven.

### 3.2 Framework

**Keuze: Spring Boot 3.** Standaard voor Java backend, goede integratie met RabbitMQ en HAPI FHIR.

### 3.3 Database

**Keuze: PostgreSQL.** Beste encryptie-ondersteuning via pgcrypto, past bij het relationele datamodel.

### 3.4 Monitoring

| Criterium | G | OpenTelemetry + Grafana | Datadog | Alleen logging |
|-----------|---|------------------------|---------|----------------|
| Leergemak voor het team | 5 | 3/5 | 4/5 | 5/5 |
| Integratie met Spring Boot | 5 | 5/5 | 4/5 | 3/5 |
| Real-time dashboard | 4 | 5/5 | 5/5 | 1/5 |
| Open standaard | 4 | 5/5 | 2/5 | 4/5 |
| Complexiteit van opzetten | 3 | 3/5 | 5/5 | 5/5 |
| Documentatie en community | 2 | 5/5 | 4/5 | 3/5 |
| **Totaalscore** | | **99/115** | 91/115 | 81/115 |

**Keuze: OpenTelemetry + Grafana.** Open standaard, geen vendor lock-in, Spring Boot 3 ondersteunt het via Micrometer.

---

## 4. ADR 3 — Hoe koppelen we aan OpenMRS?

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 22-4-2026 |
| Besloten door | Hele projectgroep |

### Opties

**Optie A: FHIR REST API polling (onze keuze)**
De module vraagt op vaste momenten de FHIR API van OpenMRS om nieuwe afspraken.

Voordelen:
- Volledig FHIR R4 standaard, werkt met elke OpenMRS 2.7+.
- Eenvoudig te implementeren met Spring Scheduler en HAPI FHIR.
- Makkelijk te schalen: per organisatie URL en credentials opslaan.
- Bij downtime van OpenMRS mist de module maximaal het polling-interval.

Nadeel: niet real-time. Voor notificaties 24u/1u van tevoren is dat geen probleem.

**Optie B: Atom Feed**
Oud mechanisme, niet FHIR-conform, niet altijd actief. Valt af.

**Optie C: Webhooks (push vanuit OpenMRS)**
Real-time, maar als de module offline is gaan pushberichten verloren. OpenMRS heeft geen ingebouwde retry. Te onbetrouwbaar.

### Beslissing
**FHIR REST API polling.**

### Scenario's

| Scenario | Wat er gebeurt |
|----------|----------------|
| OpenMRS is offline | Poll mislukt, fout gelogd, volgende cyclus opnieuw. Bekende afspraken worden gewoon genotificeerd. |
| Messaging provider is offline | Bericht naar dead letter queue van RabbitMQ. Retry met pauze. Zichtbaar in dashboard. |
| Module is offline | Na herstart hervat de polling. Afspraken die al voorbij zijn worden overgeslagen. |
| Nieuw ziekenhuis koppelen | Beheerder voegt organisatie toe. Scheduler pikt het op bij de volgende cyclus. |

---

## 5. Samenwerking en projectmanagement

### Taakverdeling

| Laag | Verantwoordelijke |
|------|------------------|
| Scheduling en polling (FHIR ophalen) | Boyan |
| Provider-adapters (SwiftSend, LegacyLink, etc.) | Jeroen |
| HL7/FHIR-verwerking en validatie | Luc |
| Monitoring en dashboard | Koen |
| Database, encryptie en dataopruiming | Koen |
| Docker, CI | Luc |
| Testen | Iedereen zijn eigen laag |

### Werkafspraken

- Feature branches per user story: `feature/US-[nummer]-[korte-naam]`
- Bugfixes: `fix/[korte-naam]`
- Niemand commit direct op main — alles via pull request met minimaal 1 goedkeuring van iemand buiten de eigen laag.
- Commit-berichten in gebiedende wijs: bijv. `Voeg retry toe aan polling service`

### Voortgang bijhouden

- Scrumbord in GitHub Projects: To Do → In Progress → Review → Done
- Elke werkdag een standup van max 15 minuten
- Taak langer dan 2 dagen op In Progress zonder wijziging → bespreken in standup
- Scrum Master roteert per sprint

### Kleine beslissingen

- Niet groot genoeg voor ADR → commentaar in code met label `// BESLISSING:`
- Grotere maar geen ADR-waardige keuzes → `TECHNICAL-LOG.md` met datum, onderwerp, keuze en reden

---

## Bijlage — Niet-functionele eisen en gevolgen

| Eis | Gevolg voor de module |
|-----|----------------------|
| AES-256 opslag, TLS 1.3 transport | pgcrypto in PostgreSQL + TLS-configuratie in Spring Boot |
| Zelfstandig proces, los van andere systemen | Zie ADR 1: zelfstandige module |
| Downtime providers opvangen | RabbitMQ dead letter queue + exponential backoff |
| Meerdere OpenMRS-instanties ondersteunen | Polling per organisatie, zie ADR 3 |
| HL7/FHIR standaard | HAPI FHIR bibliotheek, zie ADR 2 en 3 |
| Real-time monitoring dashboard | OpenTelemetry + Prometheus + Grafana, zie ADR 2 |
| Patiëntdata verwijderen binnen 14 dagen | Geplande opruimtaak in de module |
| Metadata maximaal 1 jaar bewaren | Aparte tabel zonder persoonsgegevens in PostgreSQL |
