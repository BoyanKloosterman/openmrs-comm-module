# FHIR-validatie (US-009) — beheerders

De communicatiemodule valideert inkomende **FHIR R5**-berichten en resources uit de **FHIR-poll/sync** voordat ze worden verwerkt of opgeslagen. Ongeldige data wordt geweigerd en gelogd.

---

## Waar geldt validatie?

| Pad | Wat wordt gevalideerd |
|-----|----------------------|
| `POST /api/fhir/messages` | Volledige `Bundle` (structuur + Patient/Appointment entries) |
| FHIR-poll (HAPI R5) | Losse `Patient`- en `Appointment`-resources vóór mapping |
| OpenMRS→FHIR sync | `Patient`/`Appointment` bij export naar HAPI (moet voldoen vóór upsert) |

HTTP-verkeer voor FHIR-endpoints gebruikt `application/json` en `application/fhir+json` via de **FHIR R5 HttpMessageConverter**.

---

## Verplichte velden

### Patient

| Veld | Regel |
|------|--------|
| `id` | Aanwezig; alleen tekens `A–Z`, `a–z`, `0–9`, `-`, `.` (max. 64) |
| `name` | Minimaal één `name` met `given` en/of `family` |
| `gender` **of** `birthDate` | Minimaal één van beide |
| `telecom` | Minimaal één telefooncontact: `system` = `phone`, `sms` of `other` (of geen system), met niet-lege `value` |

### Appointment

| Veld | Regel |
|------|--------|
| `id` | Zelfde id-regels als Patient |
| `start` | Aanwezig en parsebaar als datum/tijd |
| Patient-referentie | Via `subject.reference` of `participant.actor.reference` als `Patient/{id}` met geldig id-deel |

### Bundle (inkomende berichten)

| Veld | Regel |
|------|--------|
| `type` | Verplicht (bijv. `transaction`, `collection`) |
| `entry` | Minimaal één entry |
| Inhoud | Minimaal één entry met `Patient` of `Appointment` |
| Lege bundle | **Niet geldig** (geen entries of alleen niet-ondersteunde resources) |

---

## Gedrag bij fouten

- **API (`/api/fhir/messages`)**: HTTP **400** met FHIR `OperationOutcome` (NACK); parsefouten ook 400, geen 500.
- **Poll**: ongeldige Appointment/Patient wordt **overgeslagen**; teller “skipped” in poll-diagnostiek; waarschuwing in logs.
- **Sync**: rij zonder telefoon én zonder `OPENMRS_SCHEDULING_SYNC_FALLBACK_PHONE` → export faat met fout in log.

---

## Configuratie (sync / poll)

| Variabele | Doel |
|-----------|------|
| `OPENMRS_SCHEDULING_SYNC_FALLBACK_PHONE` | Telefoon voor patiënten zonder attribuut in OpenMRS (verplicht voor geldige FHIR-export als DB geen nummer heeft) |
| `OPENMRS_SCHEDULING_SYNC_ENABLED` | Sync naar HAPI R5 aan/uit |
| `OPENMRS_FHIR_POLL_MODE` | `fhir` = poll via HAPI; JDBC-fallback bij FHIR-fout |

Bij sync zet de module `gender` op `unknown` als OpenMRS JDBC geen gender levert (voldoet aan “gender of birthDate”).

---

## Voorbeeld geldige Patient (JSON)

```json
{
  "resourceType": "Patient",
  "id": "pat-001",
  "name": [{ "family": "Jansen", "given": ["Jan"] }],
  "gender": "male",
  "telecom": [{ "system": "phone", "value": "+31612345678" }]
}
```

## Voorbeeld geldige Bundle (transaction)

Minimaal één Patient- en/of Appointment-entry met bovenstaande regels; zie ook [README — FHIR ACK/NACK](../README.md#voorbeeldrequests-oplossing-in-werking).

---

## Logs controleren

Zoek in applicatielogs naar:

- `validatie mislukt`
- `Patient … validatie mislukt`
- `Appointment … validatie mislukt`
- `FHIR Bundle bevat geen entries`

Bij Docker: `docker compose logs -f comm-module`

---

## Gerelateerde documentatie

- [User story US-009](user-stories.md) — acceptatiecriteria
- [ADR-3 — koppeling OpenMRS](ADR-3-hoe-koppelen-we-aan-openmrs.md)
- [Docker scheduling test](docker-scheduling-test.md)
