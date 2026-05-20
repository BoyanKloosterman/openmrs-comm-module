# Samenwerking en projectmanagement

Versie 1.0 | Groepsproject HBO Software Engineering

## Taakverdeling

| Laag | Verantwoordelijke |
|------|------------------|
| Scheduling en polling (FHIR ophalen) | Boyan |
| Provider-adapters (SwiftSend, LegacyLink, etc.) | Jeroen |
| HL7/FHIR-verwerking en validatie | Luc |
| Monitoring en dashboard | Koen |
| Database, encryptie en dataopruiming | Koen |
| Docker, CI | Luc |
| Testen | Iedereen zijn eigen laag |

## Werkafspraken

- Feature branches per user story: `feature/US-[nummer]-[korte-naam]`
- Bugfixes: `fix/[korte-naam]`
- Niemand commit direct op main — alles via pull request met minimaal 1 goedkeuring van iemand buiten de eigen laag.
- Commit-berichten in gebiedende wijs: bijv. `Voeg retry toe aan polling service`

## Voortgang bijhouden

- Scrumbord in GitHub Projects: To Do → In Progress → Review → Done
- Elke werkdag een standup van max 15 minuten
- Taak langer dan 2 dagen op In Progress zonder wijziging → bespreken in standup
- Scrum Master roteert per sprint

## Kleine beslissingen

- Niet groot genoeg voor ADR → commentaar in code met label `// BESLISSING:`
- Grotere maar geen ADR-waardige keuzes → `TECHNICAL-LOG.md` met datum, onderwerp, keuze en reden
