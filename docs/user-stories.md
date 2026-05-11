# User Stories — OpenMRS Communicatiemodule

Sprint 2 | HBO Software Engineering | Groep C8

## Taakverdeling

| Naam | Verantwoordelijkheid |
|------|---------------------|
| **Boyan** | Scheduling, polling en FHIR ophalen |
| **Jeroen** | Provider-adapters (SwiftSend, LegacyLink, AsyncFlow, SecurePost) |
| **Koen** | Database, encryptie en monitoring |
| **Luc** | HL7/FHIR-verwerking, Docker en CI |

---

## Epic 1 — Afspraaknotificaties

### US-001 — Afspraakherinnering 24 uur vooraf (Boyan)

**User story:** Als patiënt wil ik 24 uur voor mijn afspraak een bericht ontvangen zodat ik mijn ziekenhuisbezoek op tijd kan voorbereiden.

**Acceptatiecriteria:**
- Het systeem verstuurt automatisch een notificatie 24 uur voor de afspraak.
- De notificatie bevat datum, tijd, locatie en eventuele instructies.
- De notificatie wordt niet verstuurd als de afspraak al begonnen is.
- Het systeem logt of het bericht succesvol is verzonden.

---

### US-002 — Afspraakherinnering 1 uur vooraf (Boyan)

**User story:** Als patiënt wil ik 1 uur voor mijn afspraak een bericht ontvangen zodat ik op tijd kan vertrekken en niet te laat kom.

**Acceptatiecriteria:**
- Het systeem verstuurt automatisch een notificatie 1 uur voor de afspraak.
- De notificatie bevat dezelfde informatie als de 24-uurs herinnering.
- De notificatie wordt niet verstuurd als de afspraak al begonnen is.
- Als de 24-uurs notificatie al verstuurd is, wordt de 1-uurs notificatie alsnog verstuurd.
- Het systeem logt of het bericht succesvol is verzonden.

---

### US-003 — Afspraakdata ophalen uit OpenMRS (Boyan)

**User story:** Als systeem wil ik afspraakdata periodiek ophalen uit OpenMRS via de FHIR API zodat de communicatiemodule altijd actuele afspraken heeft om notificaties op te baseren.

**Acceptatiecriteria:**
- De module pollt de FHIR Appointment-resource van elke gekoppelde OpenMRS-instantie.
- Nieuwe en gewijzigde afspraken worden opgeslagen in de eigen database.
- Afspraken die al voorbij zijn worden niet opnieuw verwerkt.
- Als OpenMRS offline is, logt de module de fout en probeert het bij de volgende cyclus opnieuw.
- De polling-interval is configureerbaar per organisatie.

---

## Epic 2 — Messaging providers

### US-004 — Bericht versturen via SwiftSend (Jeroen)

**User story:** Als OpenMRS-organisatie wil ik berichten laten versturen via SwiftSend zodat mijn patiënten een notificatie ontvangen via dit platform.

**Acceptatiecriteria:**
- De module kan een bericht versturen via de SwiftSend API.
- De adapter verwerkt een succesvol antwoord van SwiftSend correct.
- Bij een foutmelding van SwiftSend wordt het bericht in de wachtrij geplaatst voor een retry.
- De adapter logt het resultaat van elke verzendpoging.

---

### US-005 — Bericht versturen via LegacyLink (Jeroen)

**User story:** Als OpenMRS-organisatie wil ik berichten laten versturen via LegacyLink zodat mijn patiënten een notificatie ontvangen via dit platform.

**Acceptatiecriteria:**
- De module kan een bericht versturen via de LegacyLink API.
- De adapter verwerkt een succesvol antwoord van LegacyLink correct.
- Bij een foutmelding wordt het bericht in de wachtrij geplaatst voor een retry.
- De adapter logt het resultaat van elke verzendpoging.

---

### US-006 — Bericht versturen via AsyncFlow (Jeroen)

**User story:** Als OpenMRS-organisatie wil ik berichten laten versturen via AsyncFlow zodat mijn patiënten een notificatie ontvangen via dit platform.

**Acceptatiecriteria:**
- De module kan een bericht versturen via de AsyncFlow API.
- De adapter verwerkt een succesvol antwoord van AsyncFlow correct.
- Bij een foutmelding wordt het bericht in de wachtrij geplaatst voor een retry.
- De adapter logt het resultaat van elke verzendpoging.

---

### US-007 — Bericht versturen via SecurePost (Jeroen)

**User story:** Als OpenMRS-organisatie wil ik berichten laten versturen via SecurePost zodat mijn patiënten een notificatie ontvangen via dit platform.

**Acceptatiecriteria:**
- De module kan een bericht versturen via de SecurePost API.
- De adapter verwerkt een succesvol antwoord van SecurePost correct.
- Bij een foutmelding wordt het bericht in de wachtrij geplaatst voor een retry.
- De adapter logt het resultaat van elke verzendpoging.

---

### US-008 — Provider configureren per organisatie (Jeroen)

**User story:** Als OpenMRS-organisatie wil ik zelf instellen welke messaging provider ik gebruik zodat ik de provider kan kiezen die past bij mijn abonnement.

**Acceptatiecriteria:**
- Een organisatie kan via de REST API een provider instellen.
- De module gebruikt de geconfigureerde provider bij het versturen van berichten.
- Het is mogelijk om meerdere providers per organisatie in te stellen.
- Provider credentials worden versleuteld opgeslagen.

---

## Epic 3 — HL7/FHIR verwerking

### US-009 — FHIR-berichten valideren (Luc)

**User story:** Als systeem wil ik binnenkomende FHIR-berichten valideren op structuur en verplichte velden zodat ongeldige berichten niet verwerkt worden en fouten vroeg gesignaleerd worden.

**Acceptatiecriteria:**
- Het systeem controleert of een FHIR Appointment-resource alle verplichte velden bevat.
- Een ongeldig bericht wordt geweigerd en de fout wordt gelogd.
- Een geldig bericht wordt doorgegeven aan de schedulinglaag.
- De validatie volgt de HL7 FHIR R4 specificatie.

---

### US-010 — ACK-berichten verwerken (Luc)

**User story:** Als systeem wil ik een acknowledgement sturen na ontvangst van een FHIR-bericht zodat de verzender weet of het bericht goed ontvangen is.

**Acceptatiecriteria:**
- Het systeem stuurt een ACK terug na succesvolle verwerking.
- Het systeem stuurt een NACK terug bij een fout of ongeldig bericht.
- De ACK bevat de juiste berichtidentificatie.

---

### US-011 — Diverse karaktersets ondersteunen (Luc)

**User story:** Als systeem wil ik berichten kunnen verwerken in diverse karaktersets zodat organisaties wereldwijd de module kunnen gebruiken ongeacht hun taal.

**Acceptatiecriteria:**
- Het systeem verwerkt berichten in UTF-8.
- Speciale tekens uit niet-Latijnse alfabetten worden correct opgeslagen en verstuurd.
- De tijdzone van de organisatie wordt meegenomen in de berichtinhoud.

---

## Epic 4 — Beveiliging en opslag

### US-012 — Gevoelige data versleuteld opslaan (Koen)

**User story:** Als organisatie wil ik dat mijn credentials en patiëntdata versleuteld worden opgeslagen zodat deze gegevens niet bruikbaar zijn bij onbevoegde toegang.

**Acceptatiecriteria:**
- Alle gevoelige gegevens worden versleuteld opgeslagen met AES-256.
- Transport naar externe systemen verloopt via TLS 1.3.
- Credentials worden nooit in logs of configuratiebestanden opgeslagen.
- Encryptiesleutels worden apart beheerd en niet in de database opgeslagen.

---

### US-013 — Patiëntdata automatisch verwijderen na 14 dagen (Koen)

**User story:** Als systeem wil ik patiënt- en afspraakgegevens automatisch verwijderen na 14 dagen zodat de module voldoet aan de AVG-bewaartermijn.

**Acceptatiecriteria:**
- Het systeem verwijdert patiëntgegevens automatisch 14 dagen na afhandeling.
- Meta-informatie zonder persoonsgegevens blijft maximaal 1 jaar bewaard.
- Het verwijderingsproces wordt gelogd.
- De opruimtaak draait automatisch zonder handmatige actie.

---

### US-014 — Meta-informatie bewaren voor factuurcontrole (Koen)

**User story:** Als organisatie wil ik meta-informatie van verstuurde berichten maximaal 1 jaar bewaren zodat ik facturen van messaging providers kan controleren.

**Acceptatiecriteria:**
- Het systeem slaat meta-informatie op zonder directe patiëntidentificatie.
- De meta-informatie bevat voldoende gegevens om facturen te controleren.
- Na 1 jaar wordt de meta-informatie automatisch verwijderd.

---

## Epic 5 — Monitoring en betrouwbaarheid

### US-015 — Real-time dashboard voor beheerders (Koen)

**User story:** Als OpenMRS-beheerder wil ik een real-time dashboard zien met de status van berichten zodat ik snel kan zien of de module goed werkt en kan ingrijpen bij fouten.

**Acceptatiecriteria:**
- Het dashboard toont de status van verstuurde berichten (succesvol, mislukt, in wachtrij).
- Het dashboard toont de throughput en eventuele foutmeldingen.
- Het dashboard werkt op basis van OpenTelemetry en Grafana.
- Het dashboard is bereikbaar via een webbrowser.

---

### US-016 — Retry bij downtime van provider (Koen)

**User story:** Als systeem wil ik een bericht automatisch opnieuw proberen als een provider offline is zodat berichten niet verloren gaan bij tijdelijke storingen.

**Acceptatiecriteria:**
- Bij een mislukte verzendpoging wordt het bericht in de RabbitMQ dead letter queue geplaatst.
- Het systeem probeert het bericht opnieuw met een exponential backoff.
- Na een configureerbaar maximum aantal pogingen wordt de fout gelogd en het bericht gearchiveerd.
- De retry-status is zichtbaar in het dashboard.

---

## Epic 6 — Afspraak annuleren

### US-017 — Geen notificatie sturen bij geannuleerde afspraak (Boyan)

**User story:** Als arts wil ik dat er geen notificatie verstuurd wordt als een afspraak geannuleerd is zodat patiënten geen verwarrende berichten ontvangen voor afspraken die niet doorgaan.

**Acceptatiecriteria:**
- Het systeem controleert bij elke poll of de status van een afspraak gewijzigd is.
- Als een afspraak de status 'cancelled' heeft, worden geplande notificaties verwijderd.
- Als een notificatie al verstuurd is voor annulering, wordt dit gelogd.
- Er wordt geen nieuwe notificatie verstuurd voor geannuleerde afspraken.

---

## Epic 7 — Infrastructuur en CI

### US-018 — Applicatie opstarten met Docker Compose (Luc)

**User story:** Als ontwikkelaar of beoordelaar wil ik de hele applicatie opstarten met één commando zodat ik niet handmatig elke service apart hoef te installeren.

**Acceptatiecriteria:**
- `docker compose up` start de communicatiemodule, PostgreSQL, RabbitMQ en OpenMRS.
- De README beschrijft het opstartcommando en een voorbeeldrequest.
- De applicatie is bereikbaar na opstarten zonder extra configuratie.
- Omgevingsvariabelen worden geladen vanuit een `.env` bestand.

---

### US-019 — Automatische tests draaien bij elke commit (Luc)

**User story:** Als ontwikkelaar wil ik dat tests automatisch draaien bij elke commit zodat ik direct weet of mijn wijziging iets kapot maakt.

**Acceptatiecriteria:**
- Een CI-pipeline draait automatisch bij elke push naar een feature branch.
- De pipeline voert alle unit tests uit.
- De pipeline faalt als een test niet slaagt.
- Het resultaat is zichtbaar in GitHub.
