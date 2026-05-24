# ADR 4 — Hoe richten we RabbitMQ in?

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 11-5-2026 |
| Besloten door | Hele projectgroep |

## Context

De communicatiemodule moet betrouwbaar berichten versturen naar externe messaging providers, ook bij tijdelijke storingen, time-outs en downtime. De opdracht vereist retry-mechanismen, dead letter queue-ondersteuning, betrouwbare aflevering en schaalbaarheid naar meerdere organisaties.

RabbitMQ is in [ADR 2](ADR-2-welke-technologie-gebruiken-we.md) gekozen als berichtenwachtrij: ondersteuning voor retries en DLQ, eenvoudiger dan Kafka voor ons volume, goede Spring Boot-integratie en betrouwbare aflevering. Notificaties hoeven niet op milliseconde-niveau real-time te zijn, maar moeten wel gegarandeerd aankomen.

## Opties

**Optie A: één centrale queue voor alles**

Alle berichten van alle organisaties en providers in één queue.

Voordelen: eenvoudig op te zetten, minder configuratie.

Nadelen: moeilijker debuggen, problemen bij één provider beïnvloeden alles, geen scheiding per provider. Valt af.

**Optie B: één queue per provider (onze keuze)**

Elke messaging provider krijgt een eigen hoofdqueue, retry queue en dead letter queue.

Voordelen:
- Problemen bij één provider beïnvloeden de rest niet
- Overzichtelijker monitoring (zie [ADR 5](ADR-5-observability.md))
- Eenvoudiger schalen en retries per provider configureren
- Duidelijke logging en foutanalyse

Nadelen: meer configuratie en queues. Acceptabel voor de gewenste betrouwbaarheid.

**Optie C: één queue per organisatie**

Volledige scheiding per ziekenhuis, maar bij veel klanten explosie aan queues, complex beheer en hogere kosten. Organisaties scheiden we in de applicatielaag (organisatie-ID op het bericht); provider is de belangrijkste verwerkingsas. Valt af.

## Beslissing

**Optie B: één queue per messaging provider**, met retry queues en dead letter queues.

## Inrichting

### Exchanges

| Exchange | Doel |
|----------|------|
| `provider.exchange` | Hoofdqueues per provider |
| `notification.retry.exchange` | Retry queues met vertraagde terugkeer |
| `provider.dead-letter.exchange` | Dead letter queues |

### Hoofdqueues

| Queue | Provider |
|-------|----------|
| `queue.swiftsend` | SwiftSend |
| `queue.securepost` | SecurePost |
| `queue.legacylink` | LegacyLink |
| `queue.asyncflow` | AsyncFlow |

De scheduler plaatst berichten in de queue van de provider die een organisatie gebruikt (`RabbitMqProducer` → `provider.exchange`).

### Retry queues

Bij een tijdelijke fout publiceert de consumer het bericht naar `retry.{provider}` via `notification.retry.exchange`. De retry queue heeft een dead-letter route terug naar de hoofdqueue (`provider.exchange`).

De wachttijd wordt per bericht gezet via **message expiration** (TTL), berekend in de applicatie met **exponential backoff**:

| Instelling | Standaard |
|------------|-----------|
| `messaging.retry.max-attempts` | 3 |
| `messaging.retry.initial-delay-ms` | 5000 |
| `messaging.retry.multiplier` | 2 |
| `messaging.retry.max-delay-ms` | 60000 |

Voorbeeld wachttijden: poging 1 → 5 s, poging 2 → 10 s, poging 3 → 20 s (afgerond naar boven, max. 60 s).

### Dead letter queues (DLQ)

Na het maximum aantal retries probeert de consumer een **volgende provider** uit de organisatie-keten. Lukt dat niet, dan volgt `AmqpRejectAndDontRequeueException` en stuurt RabbitMQ het bericht naar `dlq.{provider}`.

| DLQ | Provider |
|-----|----------|
| `dlq.swiftsend` | SwiftSend |
| `dlq.securepost` | SecurePost |
| `dlq.legacylink` | LegacyLink |
| `dlq.asyncflow` | AsyncFlow |

Berichten in de DLQ worden niet automatisch opnieuw geprobeerd. Beheerders inspecteren ze via RabbitMQ Management UI of de delivery log.

### Betrouwbaarheid

- **Durable queues** en **persistent messages**
- **Manual ack** via Spring AMQP: bericht verdwijnt pas na succesvolle verwerking
- **Dead letter exchanges** op hoofdqueues
- **Idempotentie**: consumer slaat berichten over zonder actieve `QUEUED`-status in de delivery log

## Berichtstroom

1. Polling haalt afspraken op via FHIR ([ADR 3](ADR-3-hoe-koppelen-we-aan-openmrs.md)).
2. Scheduler bepaalt provider en publiceert naar de juiste hoofdqueue.
3. Consumer verstuurt naar de provider-API.
4. Bij succes: ack, delivery log bijgewerkt.
5. Bij tijdelijke fout: retry queue met backoff.
6. Na max retries: volgende provider in de keten, of DLQ.
7. Status zichtbaar in Grafana en delivery log ([ADR 5](ADR-5-observability.md)).

## Gevolgen

- RabbitMQ draait in de zelfstandige SaaS-module ([ADR 1](ADR-1-zelfstandige-module-of-ingebouwde-module.md)).
- Docker-compose bevat RabbitMQ met management UI (poort 15672).
- Meer infrastructuur en configuratie dan zonder wachtrij; acceptabel omdat betrouwbaarheid prioriteit heeft.
- Nieuwe providers vereisen een hoofdqueue, retry queue, DLQ en bindings in `RabbitMqConfig`.
