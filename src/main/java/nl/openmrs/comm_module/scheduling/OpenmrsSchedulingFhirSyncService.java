package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.sync.OpenmrsFhirResourceFactory;
import nl.openmrs.comm_module.sync.OpenmrsSchedulingAppointmentRow;
import nl.openmrs.comm_module.sync.OpenmrsSchedulingJdbcRepository;
import nl.openmrs.comm_module.sync.persistence.OpenmrsAppointmentFhirSyncEntity;
import nl.openmrs.comm_module.sync.persistence.OpenmrsAppointmentFhirSyncRepository;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Exporteert OpenMRS Appointment Scheduling → FHIR R5; daarna pikt {@link OpenmrsFhirPollingService}
 * de resources op in {@code polled_appointment} voor herinneringen.
 */
@Component
public class OpenmrsSchedulingFhirSyncService {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsSchedulingFhirSyncService.class);

    private final OpenmrsSchedulingSyncProperties properties;
    private final OpenmrsSchedulingJdbcRepository schedulingRepository;
    private final OpenmrsFhirResourceFactory resourceFactory;
    private final OpenmrsFhirOperations fhirOperations;
    private final OpenmrsAppointmentFhirSyncRepository syncRepository;
    private final Clock clock;

    public OpenmrsSchedulingFhirSyncService(
            OpenmrsSchedulingSyncProperties properties,
            OpenmrsSchedulingJdbcRepository schedulingRepository,
            OpenmrsFhirResourceFactory resourceFactory,
            OpenmrsFhirOperations fhirOperations,
            OpenmrsAppointmentFhirSyncRepository syncRepository,
            Clock clock) {
        this.properties = properties;
        this.schedulingRepository = schedulingRepository;
        this.resourceFactory = resourceFactory;
        this.fhirOperations = fhirOperations;
        this.syncRepository = syncRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "#{@openmrsSchedulingSyncProperties.delayMillis()}")
    public void syncOpenmrsAppointmentsToFhir() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            int synced = runSync();
            if (synced > 0) {
                log.info("OpenMRS→FHIR sync: {} afspraak(en) geëxporteerd", synced);
            }
        } catch (RuntimeException e) {
            log.error("OpenMRS→FHIR sync mislukt: {}", e.toString(), e);
        }
    }

    /** Handmatig/sync-tick: exporteert gewijzigde OpenMRS-afspraken naar FHIR. */
    @Transactional
    public int runSync() {
        ZoneId dbZone = properties.effectiveDbZoneId();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), dbZone);
        LocalDateTime from = now.minusDays(Math.max(0, properties.getLookbackDays()));
        LocalDateTime to = now.plusDays(Math.max(1, properties.getLookaheadDays()));

        List<OpenmrsSchedulingAppointmentRow> rows = schedulingRepository.findAppointmentsBetween(from, to);
        int synced = 0;
        for (OpenmrsSchedulingAppointmentRow row : rows) {
            if (row.patientUuid() == null || row.patientUuid().isBlank()) {
                log.warn("OpenMRS appointment {} overgeslagen: geen patient uuid", row.appointmentId());
                continue;
            }
            if (needsSync(row)) {
                exportRow(row);
                synced++;
            }
        }
        return synced;
    }

    private boolean needsSync(OpenmrsSchedulingAppointmentRow row) {
        return syncRepository
                .findByOpenmrsAppointmentId(row.appointmentId())
                .map(e -> !row.syncToken().equals(e.getLastSyncToken()))
                .orElse(true);
    }

    private void exportRow(OpenmrsSchedulingAppointmentRow row) {
        Patient patient = resourceFactory.buildPatient(row, properties);
        Appointment appointment = resourceFactory.buildAppointment(row, properties);

        fhirOperations.upsertPatient(patient);
        fhirOperations.upsertAppointment(appointment);

        OpenmrsAppointmentFhirSyncEntity sync = syncRepository
                .findByOpenmrsAppointmentId(row.appointmentId())
                .orElseGet(OpenmrsAppointmentFhirSyncEntity::new);
        sync.setOpenmrsAppointmentId(row.appointmentId());
        sync.setFhirAppointmentId(row.fhirAppointmentId());
        sync.setFhirPatientId(row.fhirPatientId());
        sync.setLastSyncToken(row.syncToken());
        sync.setLastSyncedAt(Instant.now(clock));
        syncRepository.save(sync);

        log.debug(
                "OpenMRS appointment {} → FHIR {}/{} (status={})",
                row.appointmentId(),
                row.fhirAppointmentId(),
                row.fhirPatientId(),
                row.status());
    }
}
