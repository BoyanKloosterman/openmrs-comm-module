package nl.openmrs.comm_module.messaging.fhir.dto;

import java.time.Instant;

public record EncounterPollDto(
    // FHIR id kan de logische resource-id zijn en hoeft niet gelijk te zijn aan de OpenMRS-uuid; die staat vaak in een Identifier
    String uuid,
    String encounterId,
    String patientId,
    Instant encounterDatetime,
    String locationId,
    String encounterType,
    // voided is waar als de afspraak geannuleerd is, false als de afspraak nog niet geannuleerd is
    boolean voided
) {
   
}

