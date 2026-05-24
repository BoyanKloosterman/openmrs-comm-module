# ADR 5 — Observability

| Status | Geaccepteerd |
|--------|-------------|
| Datum | 15-5-2026 |
| Besloten door | Hele projectgroep |

## Context

OpenMRS-beheerders moeten in real-time kunnen zien of de communicatiemodule gezond draait: berichten in de wachtrij, throughput, mislukte verzendingen en fouten bij providers. De opdrachtgever eist een monitoringdashboard; de module draait als zelfstandige SaaS-applicable (zie [ADR 1](ADR-1-zelfstandige-module-of-ingebouwde-module.md)) en moet centraal te volgen zijn zonder per ziekenhuis in te loggen.

Observability omvat drie pijlers: **metrics** (cijfers over tijd), **traces** (request-keten) en **logs** (detail bij incidenten). De keuze voor de observability-stack staat deels in [ADR 2](ADR-2-welke-technologie-gebruiken-we.md); dit ADR legt de volledige aanpak vast.

## Observability-stack

| Criterium | G | OpenTelemetry + Prometheus + Grafana | Datadog (SaaS) | Alleen logging (Logback) |
|-----------|---|--------------------------------------|----------------|--------------------------|
| Leergemak voor het team | 5 | 3/5 | 4/5 | 5/5 |
| Integratie met Spring Boot | 5 | 5/5 | 4/5 | 3/5 |
| Real-time dashboard | 4 | 5/5 | 5/5 | 1/5 |
| Open standaard / geen vendor lock-in | 4 | 5/5 | 2/5 | 4/5 |
| Kosten (SaaS-opdrachtgever) | 3 | 5/5 | 2/5 | 5/5 |
| Geschikt voor multi-tenant SaaS | 3 | 4/5 | 5/5 | 2/5 |
| **Totaalscore** | | **99/115** | 86/115 | 72/115 |

**Keuze: OpenTelemetry + Prometheus + Grafana.** Zelfde lijn als ADR 2: open standaarden, geen licentiekosten, goede Spring Boot 3/4-ondersteuning via Micrometer en Actuator. Datadog biedt snellere onboarding maar past minder bij een studentproject en creëert afhankelijkheid van een commerciële agent.

## Metrics: pull vs push

| Optie | Beschrijving | Keuze |
|-------|--------------|-------|
| **Pull (Prometheus scrape)** | Prometheus haalt elke 15 s metrics op via `/actuator/prometheus`. | **Ja** |
| Push (OTLP metrics naar collector) | App pusht metrics naar OpenTelemetry Collector. | Nee — uitgeschakeld om dubbele export en localhost-warnings te voorkomen. |
| Alleen JVM/HTTP-metrics | Standaard Micrometer zonder domein-counters. | Nee — onvoldoende voor beheerders (geen provider/fout-inzicht). |

**Keuze: pull-model met Prometheus** plus **eigen business-metrics** in `MessagingMetrics`:

| Metric | Doel |
|--------|------|
| `comm_module_messages_queued_total` | Berichten naar RabbitMQ gezet |
| `comm_module_messages_dequeued_total` | Berichten uit queue gehaald |
| `comm_module_messages_processed_total` | Verwerkingsresultaat per status |
| `comm_module_messages_failed_total` | Mislukte verzendingen |
| `comm_module_message_errors_total` | Provider-fouten |
| `comm_module_messages_in_queue` | Actuele wachtrijdiepte (gauge) |

Labels: `provider`, `message_type`, `status`, `retry`. Daarnaast levert Spring Boot standaard HTTP-metrics (`http_server_requests_seconds`) voor FHIR-endpoints.

## Traces

| Optie | Beschrijving | Keuze |
|-------|--------------|-------|
| **OpenTelemetry OTLP** | Traces via `spring-boot-starter-opentelemetry` naar een collector (poort 4318). | **Ja** |
| Geen distributed tracing | Alleen logs bij fouten. | Nee — moeilijk om FHIR → queue → provider-keten te debuggen. |
| Volledige trace-backend (Jaeger/Tempo) | Collector exporteert naar persistente trace-store. | Voorbereid; in Docker-compose exporteert de collector naar `nop` (infra klaar, geen extra opslag in demo). |

**Keuze: OpenTelemetry-traces** met sampling 100% in ontwikkeling/demo. Productie kan sampling verlagen en de collector koppelen aan Jaeger of Grafana Tempo zonder app-wijziging.

## Dashboard en operationeel inzicht

| Optie | Beschrijving | Keuze |
|-------|--------------|-------|
| **Grafana + Prometheus** | Real-time grafieken: throughput, failures, queue-diepte, filters op provider en message type. | **Primair dashboard** |
| RabbitMQ Management UI | Queue-diepte, DLQ, consumers. | **Aanvullend** — geen vervanging voor bericht-status per provider. |
| `notification_delivery_log` + logmonitor-GUI | Per-bericht audittrail in PostgreSQL, zichtbaar in test-/beheer-GUI. | **Aanvullend** — detail bij incidenten, niet voor real-time trends. |
| Alleen Actuator `/health` | UP/DOWN zonder throughput. | Onvoldoende voor acceptatiecriteria US-015. |

**Keuze: Grafana als centraal real-time dashboard** (poort 3000 in Docker-compose), provisioned via `docker/grafana/provisioning/`. Beheerders zien o.a. berichten in wachtrij, throughput (sent/submitted per seconde), failures en provider errors.

## Health checks

**Keuze: Spring Actuator** met `/actuator/health` en Kubernetes-probes ingeschakeld. Docker-compose gebruikt dit als healthcheck voor de comm-module-container.

## Beslissing

| Pijler | Technologie |
|--------|-------------|
| Metrics | Micrometer + Spring Actuator, scrape via Prometheus |
| Traces | OpenTelemetry OTLP → OpenTelemetry Collector |
| Dashboard | Grafana (datasource Prometheus) |
| Health | Actuator health + probes |
| Operationeel detail | Delivery log (DB) + RabbitMQ Management UI |

## Gevolgen

- Docker-compose bevat naast de app ook **prometheus**, **grafana** en **otel-collector** (zie README poorten 9090, 3000, 4317/4318).
- Beheerders monitoren primair via Grafana; bij een specifiek mislukt bericht raadplegen ze de delivery log of RabbitMQ DLQ.
- Nieuwe providers of message types verschijnen automatisch in Grafana-filters zodra metrics met die labels binnenkomen.
- Voor productie: Grafana-credentials via omgeving (`GRAFANA_ADMIN_*`), Prometheus-retentie en eventueel trace-backend (Tempo/Jaeger) zijn deploy-specifiek; de app-export blijft hetzelfde.
- Retry- en DLQ-gedrag (zie [ADR 4](ADR-4-rabbit-mq-queues.md)) is zichtbaar via stijgende `failed_total` en queue-metrics; circuit breaker is geen aparte metric — fouten lopen via de bestaande failure-counters.
