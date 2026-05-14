package nl.openmrs.comm_module.messaging.fhir;

import nl.openmrs.comm_module.messaging.fhir.dto.EncounterPollDto;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
public class EncounterFhirMapper {

    /** Mapt FHIR R4 Encounter naar poll-DTO; leeg als resource onbruikbaar is. */
    public Optional<EncounterPollDto> mapEncounterToEncounterPollDto(Encounter encounter) {
        if (encounter == null || !encounter.hasId()) {
            return Optional.empty();
        }
        // OpenMRS Core FHIR IG: Encounter.id ↔ OMRS Encounter.uuid (niet blind eerste Identifier)
        String encounterId = encounter.getIdElement().getIdPart();
        String uuid = encounterId;
        String patientId = refTail(encounter.hasSubject() ? encounter.getSubject().getReference() : null);
        Instant encounterDatetime = null;
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            encounterDatetime = periodStartToInstant(encounter.getPeriod().getStartElement());
        }
        String locationId = null;
        if (!encounter.getLocation().isEmpty()) {
            var loc = encounter.getLocationFirstRep();
            if (loc.hasLocation()) {
                locationId = refTail(loc.getLocation().getReference());
            }
        }
        String encounterType = typeLabel(encounter.getTypeFirstRep());
        boolean voided = false;
        if (encounter.hasStatus()) {
            String statusCode = encounter.getStatus().toCode();
            voided = "cancelled".equals(statusCode) || "entered-in-error".equals(statusCode);
        }
        if (patientId == null || patientId.isBlank() || encounterDatetime == null) {
            return Optional.empty();
        }
        return Optional.of(new EncounterPollDto(
                uuid,
                encounterId,
                patientId,
                encounterDatetime,
                locationId,
                encounterType,
                voided));
    }

    /** Zonder tijd in FHIR-waarde: start van die kalenderdag UTC; anders calendar→Instant. */
    private static Instant periodStartToInstant(DateTimeType start) {
        if (start == null || !start.hasValue()) {
            return null;
        }
        if (!start.hasTime()) {
            String v = start.getValueAsString();
            if (v != null && v.length() >= 10 && v.charAt(4) == '-') {
                return LocalDate.parse(v.substring(0, 10)).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        }
        return start.getValueAsCalendar().toInstant();
    }

    private static String refTail(String reference) {
        if (reference == null) {
            return null;
        }
        int i = reference.lastIndexOf('/');
        return i >= 0 ? reference.substring(i + 1) : reference;
    }

    private static String typeLabel(CodeableConcept concept) {
        if (concept == null || concept.isEmpty()) {
            return null;
        }
        Coding c = concept.getCodingFirstRep();
        if (c.hasDisplay()) {
            return c.getDisplay();
        }
        if (c.hasCode()) {
            return c.getCode();
        }
        if (concept.hasText()) {
            return concept.getText();
        }
        return null;
    }
}
