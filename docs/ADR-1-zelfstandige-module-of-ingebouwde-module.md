# ADR 1 — Zelfstandige module of ingebouwde module?

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 22-4-2026 |
| Besloten door | Hele projectgroep |

## Opties

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

## Beslissing

**Zelfstandige SaaS-module.**

## Gevolgen

- De module heeft zijn eigen Docker-container en eigen database.
- Communicatie met OpenMRS gaat altijd via de FHIR REST API.
