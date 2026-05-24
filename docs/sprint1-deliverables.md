# Sprint 1 Deliverables — OpenMRS Communicatiemodule

Versie 1.0 | Groepsproject HBO Software Engineering

---

## 1. OpenMRS-omgeving en integratiepunten

OpenMRS is een open-source patiëntendossier systeem. Ziekenhuizen en klinieken wereldwijd gebruiken het om patiëntgegevens en afspraken bij te houden. De communicatiemodule staat buiten OpenMRS maar heeft er data van nodig.

### Integratiepunten

| Punt | Wat het is |
|------|-----------|
| FHIR REST API | De standaard API van OpenMRS 2.7+. Hieruit halen we afspraken op (Appointment-resource) en patiëntgegevens (Patient-resource) via een gewone HTTP GET. |
| Atom Feed | Een ingebouwde event-feed in OpenMRS. Overwogen maar niet gekozen (zie [ADR 3](ADR-3-hoe-koppelen-we-aan-openmrs.md)). |
| Webhook (push) | OpenMRS kan events naar onze module pushen. Overwogen, niet gekozen vanwege risico bij downtime. |
| Authenticatie | Communicatie met OpenMRS gaat via Basic Auth of een token. Per organisatie opgeslagen in de module. |
| Messaging providers | SwiftSend, LegacyLink, AsyncFlow en SecurePost. Elk met hun eigen API. |

---

## 2. Architecture Decision Records (ADR)

| ADR | Onderwerp | Document |
|-----|-----------|----------|
| 1 | Zelfstandige module of ingebouwde module? | [ADR-1-zelfstandige-module-of-ingebouwde-module.md](ADR-1-zelfstandige-module-of-ingebouwde-module.md) |
| 2 | Welke technologie gebruiken we? | [ADR-2-welke-technologie-gebruiken-we.md](ADR-2-welke-technologie-gebruiken-we.md) |
| 3 | Hoe koppelen we aan OpenMRS? | [ADR-3-hoe-koppelen-we-aan-openmrs.md](ADR-3-hoe-koppelen-we-aan-openmrs.md) |
| 4 | RabbitMQ queues | [ADR-4-rabbit-mq-queues.md](ADR-4-rabbit-mq-queues.md) |
| 5 | Observability | [ADR-5-observability.md](ADR-5-observability.md) |

---

## 3. Samenwerking en projectmanagement

Zie [samenwerking-en-projectmanagement.md](samenwerking-en-projectmanagement.md).

---

## 4. Bijlage — Niet-functionele eisen

Zie [bijlage-niet-functionele-eisen.md](bijlage-niet-functionele-eisen.md).
