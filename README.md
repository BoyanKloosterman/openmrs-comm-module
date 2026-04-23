# OpenMRS Comm Module

Spring Boot module voor OpenMRS communicatie. Gebouwd met Java 17, Spring Boot 4 en PostgreSQL.

## Vereisten

- Java 17 (JDK)
- Maven (of gebruik de meegeleverde `mvnw` wrapper)
- Docker + Docker Compose (voor PostgreSQL en RabbitMQ)

## Opstarten

### 1. Omgevingsvariabelen

Kopieer het voorbeeldbestand en vul de waarden in.

```bash
cp .env.example .env
```

### 2. Infrastructuur starten (Postgres + RabbitMQ)

```bash
docker compose -f dockercompose.yml up -d
```

### 3. Applicatie starten

Op Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Op Linux/macOS:

```bash
./mvnw spring-boot:run
```

De applicatie draait standaard op [http://localhost:8080](http://localhost:8080).

## Build

JAR bouwen zonder tests.

```bash
./mvnw clean package -DskipTests
```

Daarna draaien via.

```bash
java -jar target/comm-module-0.0.1-SNAPSHOT.jar
```

## Testen

```bash
./mvnw test
```

## Actuator

Healthcheck endpoint beschikbaar via [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

## Projectstructuur

- `src/main/java/nl/openmrs/comm_module` - applicatiecode
- `src/main/resources/application.properties` - configuratie
- `src/test/java` - tests
- `dockercompose.yml` - lokale infra (Postgres, RabbitMQ)
