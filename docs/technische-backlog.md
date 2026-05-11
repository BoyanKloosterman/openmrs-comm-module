# Technische Backlog — OpenMRS Communicatiemodule

19 user stories | 101 taken | ~56 mandagen

## Taakverdeling

| Naam | Verantwoordelijkheid |
|------|---------------------|
| **Boyan** | Scheduling, polling en FHIR ophalen |
| **Jeroen** | Provider-adapters |
| **Koen** | Database, encryptie en monitoring |
| **Luc** | HL7/FHIR-verwerking, Docker en CI |

---

## Boyan

### US-001 — Afspraakherinnering 24 uur vooraf

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 001-1 | Scheduler opzetten die elke minuut controleert welke notificaties verstuurd moeten worden | Backend | 0.5d | feature/US-001-scheduler |
| 001-2 | Query schrijven die afspraken ophaalt die over ~24 uur beginnen | Backend | 0.5d | feature/US-001-scheduler |
| 001-3 | Notificatie aanmaken met datum, tijd, locatie en instructies | Backend | 0.5d | feature/US-001-scheduler |
| 001-4 | Check inbouwen: niet versturen als afspraak al begonnen is | Backend | 0.5d | feature/US-001-scheduler |
| 001-5 | Verzendstatus loggen na elke poging | Backend | 0.5d | feature/US-001-scheduler |
| 001-6 | Unit tests schrijven voor schedulinglogica | Test | 0.5d | feature/US-001-scheduler |

### US-002 — Afspraakherinnering 1 uur vooraf

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 002-1 | Query schrijven die afspraken ophaalt die over ~1 uur beginnen | Backend | 0.5d | feature/US-002-herinnering-1u |
| 002-2 | Controleren of 24-uurs herinnering al verstuurd is voor dezelfde afspraak | Backend | 0.5d | feature/US-002-herinnering-1u |
| 002-3 | Check inbouwen: niet versturen als afspraak al begonnen is | Backend | 0.5d | feature/US-002-herinnering-1u |
| 002-4 | Unit tests schrijven voor 1-uurs scheduling | Test | 0.5d | feature/US-002-herinnering-1u |

### US-003 — Afspraakdata ophalen uit OpenMRS via FHIR polling

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 003-1 | HAPI FHIR dependency toevoegen aan pom.xml | Config | 0.5d | feature/US-003-fhir-polling |
| 003-2 | FHIR client opzetten die verbinding maakt met OpenMRS FHIR API | Backend | 0.5d | feature/US-003-fhir-polling |
| 003-3 | Polling-service schrijven met @Scheduled die elke X minuten draait | Backend | 0.5d | feature/US-003-fhir-polling |
| 003-4 | Appointment-resource parsen naar eigen Appointment datamodel | Backend | 1d | feature/US-003-fhir-polling |
| 003-5 | Patient-resource ophalen en koppelen aan afspraak | Backend | 0.5d | feature/US-003-fhir-polling |
| 003-6 | Afspraken opslaan in PostgreSQL via JPA repository | Backend | 0.5d | feature/US-003-fhir-polling |
| 003-7 | Foutafhandeling: retry bij offline OpenMRS met logging | Backend | 0.5d | feature/US-003-fhir-polling |
| 003-8 | Polling-interval configureerbaar maken per organisatie via application.properties | Config | 0.5d | feature/US-003-fhir-polling |
| 003-9 | Unit tests schrijven voor FHIR parser en polling-service | Test | 1d | feature/US-003-fhir-polling |

### US-017 — Geen notificatie bij geannuleerde afspraak

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 017-1 | Bij elke poll afspraakstatus controleren op 'cancelled' | Backend | 0.5d | feature/US-017-annulering |
| 017-2 | Geplande notificaties verwijderen als afspraak geannuleerd is | Backend | 0.5d | feature/US-017-annulering |
| 017-3 | Loggen als annulering na al verstuurde notificatie plaatsvindt | Backend | 0.5d | feature/US-017-annulering |
| 017-4 | Unit tests schrijven voor annuleringsscenario's | Test | 0.5d | feature/US-017-annulering |

---

## Jeroen

### US-004 — SwiftSend adapter bouwen

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 004-1 | MessagingProvider interface aanmaken met sendMessage() methode | Backend | 0.5d | feature/US-004-swiftsend |
| 004-2 | SwiftSendProvider klasse aanmaken die interface implementeert | Backend | 0.5d | feature/US-004-swiftsend |
| 004-3 | HTTP-client opzetten voor SwiftSend API-aanroep | Backend | 0.5d | feature/US-004-swiftsend |
| 004-4 | Succesvolle respons van SwiftSend verwerken en loggen | Backend | 0.5d | feature/US-004-swiftsend |
| 004-5 | Foutrespons verwerken en bericht in RabbitMQ plaatsen voor retry | Backend | 0.5d | feature/US-004-swiftsend |
| 004-6 | Unit tests schrijven voor SwiftSend adapter | Test | 0.5d | feature/US-004-swiftsend |

### US-005 — LegacyLink adapter bouwen

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 005-1 | LegacyLinkProvider klasse aanmaken die MessagingProvider implementeert | Backend | 0.5d | feature/US-005-legacylink |
| 005-2 | HTTP-client opzetten voor LegacyLink API-aanroep | Backend | 0.5d | feature/US-005-legacylink |
| 005-3 | Succesvolle en foutresponsen verwerken en loggen | Backend | 0.5d | feature/US-005-legacylink |
| 005-4 | Foutrespons doorsturen naar RabbitMQ dead letter queue | Backend | 0.5d | feature/US-005-legacylink |
| 005-5 | Unit tests schrijven voor LegacyLink adapter | Test | 0.5d | feature/US-005-legacylink |

### US-006 — AsyncFlow adapter bouwen

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 006-1 | AsyncFlowProvider klasse aanmaken die MessagingProvider implementeert | Backend | 0.5d | feature/US-006-asyncflow |
| 006-2 | HTTP-client opzetten voor AsyncFlow API-aanroep | Backend | 0.5d | feature/US-006-asyncflow |
| 006-3 | Succesvolle en foutresponsen verwerken en loggen | Backend | 0.5d | feature/US-006-asyncflow |
| 006-4 | Foutrespons doorsturen naar RabbitMQ dead letter queue | Backend | 0.5d | feature/US-006-asyncflow |
| 006-5 | Unit tests schrijven voor AsyncFlow adapter | Test | 0.5d | feature/US-006-asyncflow |

### US-007 — SecurePost adapter bouwen

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 007-1 | SecurePostProvider klasse aanmaken die MessagingProvider implementeert | Backend | 0.5d | feature/US-007-securepost |
| 007-2 | HTTP-client opzetten voor SecurePost API-aanroep | Backend | 0.5d | feature/US-007-securepost |
| 007-3 | Succesvolle en foutresponsen verwerken en loggen | Backend | 0.5d | feature/US-007-securepost |
| 007-4 | Foutrespons doorsturen naar RabbitMQ dead letter queue | Backend | 0.5d | feature/US-007-securepost |
| 007-5 | Unit tests schrijven voor SecurePost adapter | Test | 0.5d | feature/US-007-securepost |

### US-008 — Provider configureren per organisatie via REST API

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 008-1 | OrganisatieConfig datamodel en JPA entity aanmaken | Backend | 0.5d | feature/US-008-provider-config |
| 008-2 | REST endpoint aanmaken voor opslaan van organisatieconfiguratie (POST /config) | Backend | 0.5d | feature/US-008-provider-config |
| 008-3 | ProviderFactory bouwen die op basis van config de juiste adapter kiest | Backend | 1d | feature/US-008-provider-config |
| 008-4 | Provider credentials versleuteld opslaan via pgcrypto | Backend | 0.5d | feature/US-008-provider-config |
| 008-5 | Meerdere providers per organisatie ondersteunen | Backend | 0.5d | feature/US-008-provider-config |
| 008-6 | Unit en integratietests schrijven voor configuratie-endpoint | Test | 0.5d | feature/US-008-provider-config |

---

## Luc

### US-009 — FHIR Appointment-resource valideren

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 009-1 | HAPI FHIR validator configureren voor Appointment-resource | Backend | 0.5d | feature/US-009-fhir-validatie |
| 009-2 | Validatieservice schrijven die verplichte velden controleert | Backend | 1d | feature/US-009-fhir-validatie |
| 009-3 | Ongeldige berichten weigeren en fout loggen | Backend | 0.5d | feature/US-009-fhir-validatie |
| 009-4 | Geldige berichten doorgeven aan schedulinglaag | Backend | 0.5d | feature/US-009-fhir-validatie |
| 009-5 | Unit tests schrijven voor validatie met geldige en ongeldige FHIR-resources | Test | 1d | feature/US-009-fhir-validatie |

### US-010 — ACK en NACK berichten verwerken

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 010-1 | ACK-respons bouwen voor succesvolle berichtverwerking | Backend | 0.5d | feature/US-010-ack |
| 010-2 | NACK-respons bouwen voor fout of ongeldig bericht | Backend | 0.5d | feature/US-010-ack |
| 010-3 | Berichtidentificatie meesturen in ACK/NACK | Backend | 0.5d | feature/US-010-ack |
| 010-4 | Unit tests schrijven voor ACK en NACK scenario's | Test | 0.5d | feature/US-010-ack |

### US-011 — Diverse karaktersets en tijdzones ondersteunen

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 011-1 | Alle tekstvelden configureren als UTF-8 in database en applicatie | Config | 0.5d | feature/US-011-karaktersets |
| 011-2 | Tijdzone per organisatie opslaan in configuratie | Backend | 0.5d | feature/US-011-karaktersets |
| 011-3 | Notificatietijdstippen omrekenen naar lokale tijdzone van de organisatie | Backend | 1d | feature/US-011-karaktersets |
| 011-4 | Testen met niet-Latijnse tekens in berichtinhoud | Test | 0.5d | feature/US-011-karaktersets |

### US-018 — Docker Compose opzetten voor hele applicatie

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 018-1 | Dockerfile aanmaken voor de communicatiemodule | DevOps | 0.5d | feature/US-018-docker |
| 018-2 | Docker Compose aanmaken met module, PostgreSQL, RabbitMQ en OpenMRS | DevOps | 1d | feature/US-018-docker |
| 018-3 | .env.example aanmaken met alle benodigde omgevingsvariabelen | DevOps | 0.5d | feature/US-018-docker |
| 018-4 | Health checks toevoegen voor alle services in Docker Compose | DevOps | 0.5d | feature/US-018-docker |
| 018-5 | README schrijven met opstartinstructies en voorbeeldrequest | Docs | 0.5d | feature/US-018-docker |
| 018-6 | Testen of docker compose up alles correct opstart op een schone machine | Test | 0.5d | feature/US-018-docker |

### US-019 — CI-pipeline opzetten voor automatische tests

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 019-1 | GitHub Actions workflow aanmaken (.github/workflows/ci.yml) | DevOps | 0.5d | feature/US-019-ci |
| 019-2 | Pipeline configureren om Maven tests te draaien bij elke push | DevOps | 0.5d | feature/US-019-ci |
| 019-3 | Pipeline laten falen als een test niet slaagt | DevOps | 0.5d | feature/US-019-ci |
| 019-4 | Testresultaten zichtbaar maken in GitHub als check op pull requests | DevOps | 0.5d | feature/US-019-ci |

---

## Koen

### US-012 — Gevoelige data versleutelen met AES-256 en TLS 1.3

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 012-1 | pgcrypto extensie activeren in PostgreSQL | Config | 0.5d | feature/US-012-encryptie |
| 012-2 | Encryptie-service bouwen voor opslaan en ophalen van gevoelige velden | Backend | 1d | feature/US-012-encryptie |
| 012-3 | TLS 1.3 configureren in Spring Boot voor alle uitgaande verbindingen | Config | 0.5d | feature/US-012-encryptie |
| 012-4 | Controleren dat credentials nooit in logs verschijnen | Backend | 0.5d | feature/US-012-encryptie |
| 012-5 | Encryptiesleutels beheren via omgevingsvariabelen, niet in code | Config | 0.5d | feature/US-012-encryptie |
| 012-6 | Unit tests schrijven voor encryptie- en decryptieservice | Test | 0.5d | feature/US-012-encryptie |

### US-013 — Patiëntdata automatisch verwijderen na 14 dagen

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 013-1 | Veld aanmaken voor afhandelingsdatum op Appointment entity | Backend | 0.5d | feature/US-013-dataretentie |
| 013-2 | Geplande opruimtaak schrijven met @Scheduled die dagelijks draait | Backend | 0.5d | feature/US-013-dataretentie |
| 013-3 | Query schrijven die afspraken ouder dan 14 dagen verwijdert | Backend | 0.5d | feature/US-013-dataretentie |
| 013-4 | Verwijderingsactie loggen | Backend | 0.5d | feature/US-013-dataretentie |
| 013-5 | Unit tests schrijven voor opruimtaak | Test | 0.5d | feature/US-013-dataretentie |

### US-014 — Meta-informatie maximaal 1 jaar bewaren

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 014-1 | Aparte MessageLog tabel aanmaken zonder persoonsgegevens | Backend | 0.5d | feature/US-014-metadata |
| 014-2 | Na verzending meta-informatie opslaan in MessageLog | Backend | 0.5d | feature/US-014-metadata |
| 014-3 | Opruimtaak schrijven die logs ouder dan 1 jaar verwijdert | Backend | 0.5d | feature/US-014-metadata |
| 014-4 | Unit tests schrijven voor metadata-opslag en opruiming | Test | 0.5d | feature/US-014-metadata |

### US-015 — Real-time dashboard bouwen met OpenTelemetry en Grafana

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 015-1 | Micrometer en OpenTelemetry dependencies toevoegen aan pom.xml | Config | 0.5d | feature/US-015-monitoring |
| 015-2 | Prometheus metrics endpoint configureren (/actuator/prometheus) | Config | 0.5d | feature/US-015-monitoring |
| 015-3 | Custom metrics aanmaken voor verzonden, mislukte en in-wachtrij berichten | Backend | 1d | feature/US-015-monitoring |
| 015-4 | Prometheus en Grafana toevoegen aan Docker Compose | DevOps | 0.5d | feature/US-015-monitoring |
| 015-5 | Grafana dashboard configureren met relevante grafieken | DevOps | 1d | feature/US-015-monitoring |
| 015-6 | Testen of dashboard live updates toont bij versturen van testberichten | Test | 0.5d | feature/US-015-monitoring |

### US-016 — Retry-mechanisme met RabbitMQ dead letter queue

| # | Taak | Type | Dag | Branch |
|---|------|------|-----|--------|
| 016-1 | RabbitMQ dependency toevoegen aan pom.xml en configureren in Spring Boot | Config | 0.5d | feature/US-016-retry |
| 016-2 | Berichtenwachtrij aanmaken voor uitgaande notificaties | Backend | 0.5d | feature/US-016-retry |
| 016-3 | Dead letter queue aanmaken voor mislukte verzendpogingen | Backend | 0.5d | feature/US-016-retry |
| 016-4 | Exponential backoff retry-logica implementeren | Backend | 1d | feature/US-016-retry |
| 016-5 | Maximum aantal pogingen configureerbaar maken | Config | 0.5d | feature/US-016-retry |
| 016-6 | Retry-status zichtbaar maken in monitoring metrics | Backend | 0.5d | feature/US-016-retry |
| 016-7 | Unit tests schrijven voor retry-mechanisme | Test | 0.5d | feature/US-016-retry |
