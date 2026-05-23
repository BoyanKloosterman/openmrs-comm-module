# ADR 3 — Hoe koppelen we aan OpenMRS?

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 22-4-2026 |
| Besloten door | Hele projectgroep |

## Opties

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

## Beslissing

**FHIR REST API polling**, met **JDBC-fallback** (`patient_appointment`) wanneer FHIR2 geen `Appointment` levert (reference distro) of de FHIR-call faalt.

| Modus | Gedrag |
|-------|--------|
| `openmrs.fhir.poll-mode=fhir` (standaard) | Eerst FHIR R5; bij fout → JDBC als `openmrs.fhir.jdbc-fallback-enabled=true` en MariaDB geconfigureerd |
| `poll-mode=jdbc` | Alleen JDBC (test/legacy) |
| `openmrs.scheduling.sync.enabled=true` | SPA-rijen (JDBC) naar **HAPI FHIR R5** exporteren; OpenMRS FHIR2/R4 heeft geen Appointment |

## Scenario's

| Scenario | Wat er gebeurt |
|----------|----------------|
| OpenMRS is offline | Poll mislukt, fout gelogd, volgende cyclus opnieuw. Bekende afspraken worden gewoon genotificeerd. |
| Messaging provider is offline | Bericht naar dead letter queue van RabbitMQ. Retry met pauze. Zichtbaar in dashboard. |
| Module is offline | Na herstart hervat de polling. Afspraken die al voorbij zijn worden overgeslagen. |
| Nieuw ziekenhuis koppelen | Beheerder voegt organisatie toe. Scheduler pikt het op bij de volgende cyclus. |
