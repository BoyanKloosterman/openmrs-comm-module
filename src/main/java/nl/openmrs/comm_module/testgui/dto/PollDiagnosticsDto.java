package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

/** Resultaat van de laatste FHIR Appointment-poll (testmonitor). */
public record PollDiagnosticsDto(
        Instant polledAt,
        String organisationId,
        String fhirServerUrl,
        String fhirServerInfo,
        Instant windowFrom,
        Instant windowTo,
        int fhirRawCount,
        int mappedCount,
        int skippedUnmapped,
        int savedCount,
        int skippedPast,
        int skippedExcluded,
        boolean success,
        String errorMessage) {

    public String summary() {
        if (errorMessage != null && !errorMessage.isBlank()) {
            return "Poll mislukt: " + errorMessage;
        }
        return fhirRawCount
                + " FHIR, "
                + mappedCount
                + " gemapt"
                + (skippedUnmapped > 0 ? ", " + skippedUnmapped + " niet gemapt" : "")
                + ", "
                + savedCount
                + " opgeslagen"
                + (skippedPast > 0 ? ", " + skippedPast + " verleden" : "")
                + (skippedExcluded > 0 ? ", " + skippedExcluded + " uitgesloten" : "");
    }
}
