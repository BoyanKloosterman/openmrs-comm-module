# OpenMRS Comm Module

Spring Boot-module voor OpenMRS-communicatie. Java 17, Spring Boot 4, PostgreSQL en RabbitMQ.

## Vereisten

- Docker en Docker Compose (aanbevolen: volledige stack in één keer)
- Optioneel voor lokaal bouwen zonder container: Java 17 en Maven (of `mvnw`)

## Opstarten (aanbevolen): alles met Docker Compose

Alle services — PostgreSQL, RabbitMQ, OpenMRS-referentie en de communicatiemodule — starten met één commando in de projectmap.

### 1. Omgevingsvariabelen (verplicht voor Compose)

`docker-compose.yml` bevat geen fallbacks: alle waarden komen uit **`.env`** in dezelfde map. Zonder `.env` faalt `docker compose` bij het inlezen (geen secrets in Git).

```bash
cp .env.example .env
```

Pas in `.env` de placeholders (`changeme`) aan. Gebruikersnamen en URLs in het voorbeeld passen bij de servicenamen in Compose.

### 2. Stack bouwen en starten

Eerste keer kan OpenMrs enkele minuten nodig hebben voordat de healthcheck groen is.

```bash
docker compose up -d --build
```

Status bekijken:

```bash
docker compose ps
```

### 3. Voorbeeldrequest: notificatie op de wachtrij

De module luistert in Docker op hostpoort **8081** (containerpoort 8080).

**Linux / macOS / Git Bash:**

```bash
curl -sS -X POST http://localhost:8081/api/notifications/test
```

Verwachte respons: `Notification placed on queue`

**Windows PowerShell:**

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/notifications/test
```

Health van de app:

```text
http://localhost:8081/actuator/health
```

OpenMRS (platform **2.7.9** + FHIR2 + **Legacy UI**) staat op hostpoort **8080**: [http://localhost:8080/openmrs](http://localhost:8080/openmrs) — je krijgt een normale inlogpagina (gebruiker `Admin`, wachtwoord uit `.env`). Na een Dockerfile-wijziging: `docker compose build openmrs` en stack herstarten; zie je nog de oude kale pagina door een bestaand volume, wis het volume `openmrs_data` of gebruik `docker compose down -v` (let op: alle compose-volumes weg). RabbitMQ Management: [http://localhost:15672](http://localhost:15672) (gebruiker/wachtwoord zoals in `.env.example`).

### Stack stoppen

```bash
docker compose down
```

---

## Optioneel: applicatie lokaal met Maven (infra wel in Docker)

Alleen PostgreSQL en RabbitMQ starten (zonder OpenMRS en zonder comm-module-container):

```bash
docker compose up -d postgres rabbitmq
```

Daarna Spring Boot op de host; standaardpoort **8080**. Zet minimaal `SPRING_DATASOURCE_URL` (bijv. `jdbc:postgresql://localhost:5432/openmrs`), databasegebruikersnaam/-wachtwoord en RabbitMQ-host `localhost` in omgeving of `application-local.properties` (niet meegeleverd).

```powershell
.\mvnw.cmd spring-boot:run
```

```bash
./mvnw spring-boot:run
```

Voorbeeldrequest lokaal:

```bash
curl -sS -X POST http://localhost:8080/api/notifications/test
```

---

## Build

JAR bouwen (tests overslaan):

```bash
./mvnw clean package -DskipTests
```

```powershell
.\mvnw.cmd clean package -DskipTests
```

Draaien:

```bash
java -jar target/comm-module-0.0.1-SNAPSHOT.jar
```

## Testen

```bash
./mvnw test
```

## Projectstructuur

- `src/main/java/nl/openmrs/comm_module` — applicatiecode
- `src/main/resources/application.properties` — configuratie
- `src/test/java` — tests
- `docker-compose.yml` — volledige lokale stack
- `.env.example` — voorbeeld omgevingsvariabelen voor Compose
