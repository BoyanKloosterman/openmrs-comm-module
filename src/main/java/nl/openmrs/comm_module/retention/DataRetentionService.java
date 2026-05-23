package nl.openmrs.comm_module.retention;

import nl.openmrs.comm_module.config.DataRetentionProperties;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentExclusionEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentExclusionRepository;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataRetentionService {

    private static final String REDACTED_PATIENT_ID = "redacted";

    private final DataRetentionProperties properties;
    private final PolledAppointmentRepository polledAppointmentRepository;
    private final PolledAppointmentExclusionRepository exclusionRepository;
    private final Clock clock;

    public DataRetentionService(
            DataRetentionProperties properties,
            PolledAppointmentRepository polledAppointmentRepository,
            PolledAppointmentExclusionRepository exclusionRepository,
            Clock clock) {
        this.properties = properties;
        this.polledAppointmentRepository = polledAppointmentRepository;
        this.exclusionRepository = exclusionRepository;
        this.clock = clock;
    }

    @Transactional
    public DataRetentionResult runCleanup() {
        Instant now = clock.instant();
        int personalDays = Math.max(1, properties.getPersonalRetentionDays());
        int metadataDays = Math.max(1, properties.getMetadataRetentionDays());
        Instant personalCutoff = now.minus(personalDays, ChronoUnit.DAYS);
        Instant metadataCutoff = now.minus(metadataDays, ChronoUnit.DAYS);

        int redacted = redactPersonalData(personalCutoff, now);
        int deleted = polledAppointmentRepository.deleteOlderThan(metadataCutoff);
        return new DataRetentionResult(redacted, deleted);
    }

    private int redactPersonalData(Instant cutoff, Instant now) {
        List<PolledAppointmentEntity> candidates = polledAppointmentRepository.findByAppointmentDatetimeBefore(cutoff);
        List<PolledAppointmentEntity> changed = new ArrayList<>();
        List<PolledAppointmentExclusionEntity> exclusions = new ArrayList<>();

        for (PolledAppointmentEntity entity : candidates) {
            boolean updated = false;
            if (entity.getPatientDisplayName() != null) {
                entity.setPatientDisplayName(null);
                updated = true;
            }
            if (entity.getPatientPhone() != null) {
                entity.setPatientPhone(null);
                updated = true;
            }
            String patientId = entity.getPatientFhirId();
            if (patientId != null && !REDACTED_PATIENT_ID.equals(patientId)) {
                entity.setPatientFhirId(REDACTED_PATIENT_ID);
                updated = true;
            }

            if (updated) {
                changed.add(entity);
                if (!exclusionRepository.existsByOrganisationIdAndAppointmentFhirId(
                        entity.getOrganisationId(), entity.getAppointmentFhirId())) {
                    PolledAppointmentExclusionEntity exclusion = new PolledAppointmentExclusionEntity();
                    exclusion.setOrganisationId(entity.getOrganisationId());
                    exclusion.setAppointmentFhirId(entity.getAppointmentFhirId());
                    exclusion.setExcludedAt(now);
                    exclusions.add(exclusion);
                }
            }
        }

        if (!changed.isEmpty()) {
            polledAppointmentRepository.saveAll(changed);
        }
        if (!exclusions.isEmpty()) {
            exclusionRepository.saveAll(exclusions);
        }
        return changed.size();
    }
}
