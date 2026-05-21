package nl.openmrs.comm_module.poll.persistence;

import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.notification.voided.VoidedAppointmentCoordinator;
import nl.openmrs.comm_module.poll.AppointmentPollExclusionService;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class JpaAppointmentPollPersistence implements AppointmentPollPersistence {

    private final PolledAppointmentRepository repository;
    private final PolledAppointmentExclusionRepository exclusionRepository;
    private final AppointmentPollExclusionService pollExclusionService;
    private final VoidedAppointmentCoordinator voidedAppointmentCoordinator;
    private final Clock clock;

    public JpaAppointmentPollPersistence(
            PolledAppointmentRepository repository,
            PolledAppointmentExclusionRepository exclusionRepository,
            AppointmentPollExclusionService pollExclusionService,
            VoidedAppointmentCoordinator voidedAppointmentCoordinator,
            Clock clock) {
        this.repository = repository;
        this.exclusionRepository = exclusionRepository;
        this.pollExclusionService = pollExclusionService;
        this.voidedAppointmentCoordinator = voidedAppointmentCoordinator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void upsertPollResults(String organisationId, List<AppointmentWithPatientDto> appointmentsWithPatients) {
        Instant now = clock.instant();
        for (AppointmentWithPatientDto row : appointmentsWithPatients) {
            AppointmentPollDto a = row.appointment();
            if (exclusionRepository.existsByOrganisationIdAndAppointmentFhirId(
                    organisationId, a.appointmentId())) {
                continue;
            }
            if (isPastAppointment(a, now)) {
                pollExclusionService.excludeIfAbsent(organisationId, a.appointmentId(), now);
                continue;
            }
            PolledAppointmentEntity entity = repository
                    .findByOrganisationIdAndAppointmentFhirId(organisationId, a.appointmentId())
                    .orElseGet(PolledAppointmentEntity::new);
            boolean wasVoidedBefore = entity.getId() != null && entity.isVoided();
            applyAppointment(entity, organisationId, a, row.patient(), now);
            repository.save(entity);
            voidedAppointmentCoordinator.notifyIfVoided(entity, wasVoidedBefore);
        }
        excludePastStillStored(organisationId, now);
    }

    /** Bestaande DB-rijen die inmiddels voorbij zijn: uitsluiten zonder opnieuw te upserten. */
    private void excludePastStillStored(String organisationId, Instant now) {
        for (PolledAppointmentEntity stored :
                repository.findByOrganisationIdAndAppointmentDatetimeLessThanEqual(organisationId, now)) {
            pollExclusionService.excludeIfAbsent(organisationId, stored.getAppointmentFhirId(), now);
        }
    }

    private static boolean isPastAppointment(AppointmentPollDto appointment, Instant now) {
        Instant start = appointment.appointmentDatetime();
        return start == null || !start.isAfter(now);
    }

    private static void applyAppointment(
            PolledAppointmentEntity entity,
            String organisationId,
            AppointmentPollDto a,
            PatientPollDto patient,
            Instant lastPolledAt) {
        entity.setOrganisationId(organisationId);
        entity.setAppointmentUuid(a.uuid());
        entity.setAppointmentFhirId(a.appointmentId());
        entity.setPatientFhirId(a.patientId());
        entity.setAppointmentDatetime(a.appointmentDatetime());
        entity.setLocationId(a.locationLabel());
        entity.setAppointmentType(a.appointmentType());
        entity.setAppointmentReason(a.reason());
        entity.setVoided(a.voided());
        entity.setLastPolledAt(lastPolledAt);
        if (patient != null) {
            entity.setPatientDisplayName(patient.displayName());
            entity.setPatientPhone(patient.phone());
        } else {
            entity.setPatientDisplayName(null);
            entity.setPatientPhone(null);
        }
    }
}
