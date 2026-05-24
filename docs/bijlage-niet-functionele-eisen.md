# Bijlage — Niet-functionele eisen en gevolgen

Versie 1.0 | Groepsproject HBO Software Engineering

| Eis | Gevolg voor de module |
|-----|----------------------|
| AES-256 opslag, TLS 1.3 transport | pgcrypto in PostgreSQL + TLS-configuratie in Spring Boot |
| Zelfstandig proces, los van andere systemen | Zie [ADR 1](ADR-1-zelfstandige-module-of-ingebouwde-module.md): zelfstandige module |
| Downtime providers opvangen | RabbitMQ dead letter queue + exponential backoff, zie [ADR 4](ADR-4-rabbit-mq-queues.md) |
| Meerdere OpenMRS-instanties ondersteunen | Polling per organisatie, zie [ADR 3](ADR-3-hoe-koppelen-we-aan-openmrs.md) |
| HL7/FHIR standaard | HAPI FHIR bibliotheek, zie [ADR 2](ADR-2-welke-technologie-gebruiken-we.md) en [ADR 3](ADR-3-hoe-koppelen-we-aan-openmrs.md) |
| Real-time monitoring dashboard | OpenTelemetry + Prometheus + Grafana, zie [ADR 5](ADR-5-observability.md) |
| Patiëntdata verwijderen binnen 14 dagen | Geplande opruimtaak in de module |
| Metadata maximaal 1 jaar bewaren | Aparte tabel zonder persoonsgegevens in PostgreSQL |
