package nl.openmrs.comm_module.poll;

import nl.openmrs.comm_module.testgui.dto.PollDiagnosticsDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Houdt statistieken van de laatste FHIR-poll bij (gepland + handmatig). */
@Component
public class PollDiagnosticsRecorder {

    private final AtomicReference<Mutable> current = new AtomicReference<>();

    public void begin(String organisationId, String fhirServerUrl, Instant windowFrom, Instant windowTo) {
        Mutable m = new Mutable();
        m.polledAt = Instant.now();
        m.organisationId = organisationId;
        m.fhirServerUrl = fhirServerUrl;
        m.windowFrom = windowFrom;
        m.windowTo = windowTo;
        m.success = true;
        current.set(m);
    }

    public void setFhirServerInfo(String info) {
        withCurrent(m -> m.fhirServerInfo = info);
    }

    public void setFhirRawCount(int count) {
        withCurrent(m -> m.fhirRawCount = count);
    }

    public void setMappedCounts(int mapped, int skippedUnmapped) {
        withCurrent(
                m -> {
                    m.mappedCount = mapped;
                    m.skippedUnmapped = skippedUnmapped;
                });
    }

    public void addPersistStats(int saved, int skippedPast, int skippedExcluded) {
        withCurrent(
                m -> {
                    m.savedCount += saved;
                    m.skippedPast += skippedPast;
                    m.skippedExcluded += skippedExcluded;
                });
    }

    public void setError(String message) {
        withCurrent(
                m -> {
                    m.success = false;
                    m.errorMessage = message;
                });
    }

    public Optional<PollDiagnosticsDto> getLast() {
        Mutable m = current.get();
        return m == null ? Optional.empty() : Optional.of(m.toDto());
    }

    private void withCurrent(java.util.function.Consumer<Mutable> action) {
        Mutable m = current.get();
        if (m != null) {
            action.accept(m);
        }
    }

    private static final class Mutable {
        Instant polledAt;
        String organisationId;
        String fhirServerUrl;
        String fhirServerInfo;
        Instant windowFrom;
        Instant windowTo;
        int fhirRawCount;
        int mappedCount;
        int skippedUnmapped;
        int savedCount;
        int skippedPast;
        int skippedExcluded;
        boolean success = true;
        String errorMessage;

        PollDiagnosticsDto toDto() {
            return new PollDiagnosticsDto(
                    polledAt,
                    organisationId,
                    fhirServerUrl,
                    fhirServerInfo,
                    windowFrom,
                    windowTo,
                    fhirRawCount,
                    mappedCount,
                    skippedUnmapped,
                    savedCount,
                    skippedPast,
                    skippedExcluded,
                    success,
                    errorMessage);
        }
    }
}
