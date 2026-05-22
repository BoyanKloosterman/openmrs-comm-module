package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.sync.OpenmrsFhirResourceFactory;
import nl.openmrs.comm_module.sync.OpenmrsPatientAppointmentJdbcRepository;
import nl.openmrs.comm_module.sync.OpenmrsSchedulingAppointmentRow;
import nl.openmrs.comm_module.sync.persistence.OpenmrsAppointmentFhirSyncEntity;
import nl.openmrs.comm_module.sync.persistence.OpenmrsAppointmentFhirSyncRepository;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Export patient_appointment → FHIR R5 (optioneel; uit bij reference-distro JDBC-poll). */
@Component
@ConditionalOnProperty(name = "openmrs.scheduling.sync.enabled", havingValue = "true")
public class OpenmrsSchedulingFhirSyncService {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsSchedulingFhirSyncService.class);

    private final OpenmrsSchedulingSyncProperties properties;
    private final OpenmrsPatientAppointmentJdbcRepository appointmentRepository;
    private final OpenmrsFhirResourceFactory resourceFactory;
    private final OpenmrsFhirOperations fhirOperations;
    private final OpenmrsAppointmentFhirSyncRepository syncRepository;
    private final Clock clock;

    public OpenmrsSchedulingFhirSyncService(
            OpenmrsSchedulingSyncProperties properties,
            OpenmrsPatientAppointmentJdbcRepository appointmentRepository,
            OpenmrsFhirResourceFactory resourceFactory,
            OpenmrsFhirOperations fhirOperations,
            OpenmrsAppointmentFhirSyncRepository syncRepository,
            Clock clock) {
        this.properties = properties;
        this.appointmentRepository = appointmentRepository;
        this.resourceFactory = resourceFactory;
        this.fhirOperations = fhirOperations;
        this.syncRepository = syncRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "#{@openmrsSchedulingSyncProperties.delayMillis()}")
    public void syncOpenmrsAppointmentsToFhir() {
        try {
            int synced = runSync();
            if (synced > 0) {
                log.info("OpenMRS→FHIR sync: {} afspraak(en) geëxporteerd", synced);
            }
        } catch (RuntimeException e) {
            log.error("OpenMRS→FHIR sync mislukt: {}", e.toString(), e);
        }
    }

    @Transactional
    public int runSync() {
        if (!properties.isPatientAppointmentSource()) {
            log.warn("Sync ondersteunt alleen source=patient-appointment");
            return 0;
        }
        ZoneId dbZone = properties.effectiveDbZoneId();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), dbZone);
        LocalDateTime from = now.minusDays(Math.max(0, properties.getLookbackDays()));
        LocalDateTime to = now.plusDays(Math.max(1, properties.getLookaheadDays()));

        List<OpenmrsSchedulingAppointmentRow> rows =
                appointmentRepository.findAppointmentsBetween(from, to);
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
