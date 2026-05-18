# ADR 2 — Welke technologie gebruiken we?

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 22-4-2026 |
| Besloten door | Hele projectgroep |

## Programmeertaal

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

## Framework

**Keuze: Spring Boot 3.** Standaard voor Java backend, goede integratie met RabbitMQ en HAPI FHIR.

## Database

**Keuze: PostgreSQL.** Beste encryptie-ondersteuning via pgcrypto, past bij het relationele datamodel.

## Monitoring

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
