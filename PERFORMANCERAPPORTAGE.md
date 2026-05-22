# Performancerapportage — OpenMRS Communicatiemodule

Versie 1.1 | Sprint 3 | Meting: 22 mei 2026

Beknopte onderbouwing voor het rubricumcriterium **betrouwbaarheid**: throughput onder belasting, latency bij provider-aanroepen, en aantoonbare verbetering ten opzichte van een eerdere architectuur. Sluit aan op deliverable 4 uit [docs/sprint3-doelen.txt](docs/sprint3-doelen.txt).

---

## Samenvatting

| Metriek | Waarde (lokale Docker-stack, 22-05-2026) |
|---------|------------------------------------------|
| Queue-throughput (POST `/api/notifications/test`, 50×) | **~481 req/s** (104 ms totaal) |
| Gem. HTTP-latency enqueue | **~0,55 ms** per request (Micrometer) |
| Gem. end-to-end consumer (queue → fake provider) | **~3,7 s** per bericht (2 samples; zie toelichting) |
| Scheduler-tick (`checkDueNotifications`) | **~10 ms** gemiddeld (10 ticks) |
| FHIR-poll-tick (`pollOpenmrsFhir`) | **~1,48 s** gemiddeld (10 ticks; HAPI + veel Appointment) |
| SPA → HAPI sync-tick | **~143 ms** gemiddeld (10 ticks) |

De module is ontworpen voor **batch-herinneringen** (poll + minutely scheduler), niet voor duizenden berichten per seconde. Metingen tonen voldoende marge voor typische kliniekvolumes (honderden afspraken per dag).

---

## Testomgeving

| Onderdeel | Configuratie |
|-----------|--------------|
| Stack | `docker compose` (zie [README.md](README.md)) |
| Comm-module | `http://localhost:8081` |
| HAPI FHIR R5 | `http://localhost:8082/fhir` (`hapi-fhir-r5`, image `hapiproject/hapi:latest`) |
| OpenMRS distro | Aparte compose, poort **80**; MariaDB **3307** voor sync/fallback |
| Provider | `fakecomworld` op poort 1337 |
| Metrics | Prometheus scrape → `http://localhost:8081/actuator/prometheus` |
| Dashboard | Grafana `http://localhost:3000` (datasource Prometheus) |
| Belastingstest | PowerShell: 50× `POST /api/notifications/test` |

---

## 1. Throughput — berichten per tijdseenheid

### 1.1 Enqueue (API → RabbitMQ)

Meting: 50 opeenvolgende test-notificaties.

```
50 requests in 104 ms (~481 req/s)
```

Micrometer na de test (`http_server_requests_seconds`):

| Endpoint | Count | Sum (s) | Gemiddelde |
|----------|-------|---------|------------|
| `POST /api/notifications/test` | 50 | 0,028 | **~0,55 ms** |

Dit meet alleen **acceptatie en plaatsing op de queue** (HTTP 202), niet de provider-call.

### 1.2 Verwerking (RabbitMQ → provider)

Micrometer `spring_rabbitmq_listener_seconds` (queue `queue.swiftsend`):

| Count | Sum (s) | Gemiddelde |
|-------|---------|------------|
| 2 | 7,46 | **~3,7 s** per bericht (weinig samples tijdens deze run) |

Eerdere meting (21-05-2026, meer consumer-activiteit): **~82 ms** gemiddeld over 51 berichten. Het verschil komt door samplegrootte en eventuele cold start; beide tonen dat enqueue en scheduling **niet** wachten op de consumer.

Inclusief per bericht: deserialisatie, `MessagingProvider.sendMessage`, HTTP naar fakecomworld, schrijven delivery log.

### 1.3 Scheduler (achtergrond)

`tasks_scheduled_execution_seconds` — `checkDueNotifications`:

| Ticks | Sum (s) | Gemiddelde |
|-------|---------|------------|
| 10 | 0,102 | **~10 ms** |

Bij 1 minuut interval: verwerking van “due” afspraken uit Postgres blijft ruim binnen het interval.

### 1.4 FHIR-poll en sync (achtergrond)

| Taak | Ticks | Sum (s) | Gemiddelde |
|------|-------|---------|------------|
| `pollOpenmrsFhir` | 10 | 14,81 | **~1,48 s** |
| `syncOpenmrsAppointmentsToFhir` | 10 | 1,43 | **~143 ms** |

Poll-latency is hoger dan een kale `/metadata`-call omdat de tick **Appointment-search + Patient-reads** op HAPI uitvoert (demo met tientallen afspraken). Bij lege HAPI of klein venster daalt dit naar honderden ms (eerdere meting ~80 ms gemiddeld).

### 1.5 Extrapolatie praktijkvolume

| Scenario | Volume | Inschatting |
|----------|--------|-------------|
| Kleine poli | 50 herinneringen/uur piek | Ruim binnen queue + 4 parallelle provider-queues |
| Dagtotaal | 500 afspraken, 2 herinneringen | ~1000 berichten/24u ≈ 0,01 bericht/s gemiddeld |

De gemeten enqueue-capaciteit (**~480 req/s**) en scheduler (**~10 ms**) liggen orders of magnitude boven verwachte productielast.

---

## 2. Latency — provider-aanroepen

### 2.1 Opbouw end-to-end latency

```mermaid
sequenceDiagram
  participant API as Comm-module API
  participant Q as RabbitMQ
  participant C as RabbitMqConsumer
  participant P as MessagingProvider
  participant F as FakeComWorld
  API->>Q: publish (ms)
  Q->>C: deliver
  C->>P: sendMessage
  P->>F: HTTP
  F-->>P: response
  P-->>C: ProviderSendResult
  C->>C: delivery log SENT/FAILED
```

| Fase | Typische duur (meting) |
|------|------------------------|
| Enqueue | &lt; 1 ms |
| Queue + listener overhead | enkele ms |
| Provider HTTP (fakecomworld) | dominant (~70–80 ms bij veel samples) |
| DB log write | &lt; 5 ms |

### 2.2 Retry en backoff

Bij falen: `RabbitMqProducer.publishRetry` met exponential backoff (`messaging.retry.*`):

| Parameter | Default |
|-----------|---------|
| `max-attempts` | 3 |
| `initial-delay-ms` | 5000 |
| `multiplier` | 2 |
| `max-delay-ms` | 60000 |

Effect op latency: mislukte pogingen verlengen totale doorlooptijd bewust (resiliency), zonder de scheduler te blokkeren.

### 2.3 FHIR-poll en sync (kritiek pad, geen provider)

| Taak | Rol | Gedrag bij uitval |
|------|-----|-------------------|
| `syncOpenmrsAppointmentsToFhir` | MariaDB → HAPI R5 | Log WARN; volgende tick opnieuw |
| `pollOpenmrsFhir` | HAPI → `polled_appointment` | Bij FHIR-fout: JDBC-fallback (indien geconfigureerd) |
| Bestaande rijen | Scheduler | Blijven beschikbaar (zie [docs/ADR-3-hoe-koppelen-we-aan-openmrs.md](docs/ADR-3-hoe-koppelen-we-aan-openmrs.md)) |

---

## 3. Verbetering ten opvichte van eerdere versie

Er is geen aparte load-test-suite in de repository; verbetering wordt onderbouwd met **architectuurvergelijking** en **meetbare gedragingen** na invoering van de huidige keten.

### Baseline (vroege sprint / synchroon pad)

| Aspect | Vroeger gedrag | Risico |
|--------|----------------|--------|
| Verzending | Direct in scheduler- of poll-thread | Blokkeert ticks; FHIR/poll vertraagt bij trage provider |
| Foutafhandeling | Geen queue / beperkte retry | Bericht verloren bij korte provider-storing |
| Dubbele runs | Geen delivery-log deduplicatie | Dubbele SMS bij herhaalde scheduler-tick |
| OpenMRS-koppeling | Alleen JDBC of alleen FHIR2 R4 (geen Appointment) | Geen opdrachtconforme FHIR R5-poll op reference distro |

### Huidige versie (meting sprint 3, bijgewerkt 22-05-2026)

| Aspect | Huidige implementatie | Aantoonbaar effect |
|--------|----------------------|-------------------|
| Ontkoppeling | RabbitMQ + async consumer | Scheduler **~10 ms** vs provider **~80 ms+** — provider-latency blokkeert scheduling niet |
| Retry | Max 3 pogingen, exponential backoff | `RabbitMqConsumerTest` + configureerbare `messaging.retry.*` |
| Idempotentie | `notification_delivery_log` | Integratietest + geen dubbele publish; zie [TESTRAPPORTAGE.md](TESTRAPPORTAGE.md) |
| FHIR R5 | HAPI in compose + FHIR-poll primair | `Appointment`/`Patient` op R5; JDBC-fallback bij fout |
| Sync + tijdzone | `effectiveDbZoneId()` overal | Geen 2u verschil SPA vs logmonitor |
| Observability | `/actuator/prometheus`, Prometheus, Grafana | Throughput en latency reproduceerbaar uitleesbaar |
| TLS / pool | HikariCP, TLS 1.3 FHIR-client | DB acquire blijft laag onder testload |

### Kwantitatieve vergelijking (indicatief)

| Metriek | Baseline (indicatief synchroon) | Nu (gemeten 22-05-2026) |
|---------|--------------------------------|-------------------------|
| Scheduler-tick bij trage provider (500 ms) | ~500 ms+ geblokkeerd | **~10 ms** (provider async) |
| Herstel na provider-fout | Handmatig / verloren | Automatisch retry tot 3× |
| Dubbele herinneringzelfde tick | Mogelijk | **Voorkomen** (delivery log + tests) |
| FHIR-poll op reference distro | Niet mogelijk (geen R5 Appointment) | HAPI R5 + sync |

*Baseline-cijfers zijn engineering-inschatting op basis van het oude ontwerp; “nu” is gemeten op de draaiende stack.*

---

## 4. Monitoring en reproduceerbaarheid

### Metrics ophalen

```bash
curl -sS http://localhost:8081/actuator/prometheus
```

Prometheus-config: [docker/prometheus/prometheus.yml](docker/prometheus/prometheus.yml) (scrape-interval 15s).

### Nuttige PromQL-voorbeelden

```promql
# Gemiddelde HTTP-latency test-endpoint (5 min)
rate(http_server_requests_seconds_sum{uri="/api/notifications/test"}[5m])
/
rate(http_server_requests_seconds_count{uri="/api/notifications/test"}[5m])

# Gemiddelde consumer-latency SWIFTSEND
rate(spring_rabbitmq_listener_seconds_sum{queue="queue.swiftsend"}[5m])
/
rate(spring_rabbitmq_listener_seconds_count{queue="queue.swiftsend"}[5m])

# Scheduler-doorlooptijd
rate(tasks_scheduled_execution_seconds_sum{code_function="checkDueNotifications"}[5m])
/
rate(tasks_scheduled_execution_seconds_count{code_function="checkDueNotifications"}[5m])

# FHIR-poll (inclusief HAPI search)
rate(tasks_scheduled_execution_seconds_sum{code_function="pollOpenmrsFhir"}[5m])
/
rate(tasks_scheduled_execution_seconds_count{code_function="pollOpenmrsFhir"}[5m])
```

### Belastingtest herhalen (PowerShell)

```powershell
$sw = [System.Diagnostics.Stopwatch]::StartNew()
1..50 | ForEach-Object {
  Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/notifications/test" | Out-Null
}
$sw.Stop()
Write-Host "50 requests in $($sw.ElapsedMilliseconds) ms"
```

Daarna opnieuw `/actuator/prometheus` scrapen of Grafana-dashboard raadplegen.

### Database-controle

```powershell
docker exec comm-module-db psql -U comm_user -d comm_module -c `
  "SELECT status, COUNT(*) FROM notification_delivery_log GROUP BY status;"
```

*(Gebruik `POSTGRES_USER` / `POSTGRES_DB` uit uw `.env`.)*

---

## 5. Beperkingen en vervolg

| Beperking | Toelichting |
|-----------|-------------|
| Geen dedicated JMeter/Gatling-suite | Metingen zijn kortstondige lokale load + Micrometer |
| Fake provider ≠ productie-SMS | Absolute latency in productie hoger; relatieve verbetering (async + retry) blijft geldig |
| FHIR-poll varieert met HAPI-vulling | Meer `Appointment` in HAPI → langere poll-tick; normaal voor demo |
| HAPI geen Docker-healthcheck | Distroless image; compose start comm-module na `service_started` |
| Custom business-metrics | Backlog [US-015](docs/technische-backlog.md): aparte counters verzonden/mislukt — nu vooral Spring/Micrometer standaardmetrics |

Aanbevolen vervolg voor productie: periodieke scrape in Grafana, alerts op DLQ-diepte en stijgende `spring_rabbitmq_listener_seconds` p95.

---

## Conclusie

Onder lokale belasting verwerkt de module **honderden enqueue-requests per seconde** en blijft de **scheduler in de milliseconden**, los van provider-latency. **FHIR-poll en sync** draaien asynchroon; poll-duur schaalt met het aantal afspraken op HAPI, wat voor de beoogde dagelijkse batch-workload acceptabel is. Ten opzichte van een synchroon ontwerp zijn **ontkoppeling, retry, idempotentie, FHIR R5-keten en observability** aantoonbaar ingevoerd — dat ondersteunt betrouwbaarheid voor de herinnerings-workload.
