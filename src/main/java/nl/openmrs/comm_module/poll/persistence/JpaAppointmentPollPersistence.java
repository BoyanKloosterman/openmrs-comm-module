package nl.openmrs.comm_module.poll.persistence;

import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.notification.CancelledAppointmentNotificationService;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JpaAppointmentPollPersistence implements AppointmentPollPersistence {

    private final PolledAppointmentRepository repository;
    private final PolledAppointmentExclusionRepository exclusionRepository;
    private final CancelledAppointmentNotificationService cancelledAppointmentNotificationService;

    public JpaAppointmentPollPersistence(
            PolledAppointmentRepository repository,
            PolledAppointmentExclusionRepository exclusionRepository,
            CancelledAppointmentNotificationService cancelledAppointmentNotificationService) {
        this.repository = repository;
        this.exclusionRepository = exclusionRepository;
        this.cancelledAppointmentNotificationService = cancelledAppointmentNotificationService;
    }

    @Override
    @Transactional
    public void upsertPollResults(String organisationId, List<AppointmentWithPatientDto> appointmentsWithPatients) {
        Instant now = Instant.now();
        for (AppointmentWithPatientDto row : appointmentsWithPatients) {
            AppointmentPollDto a = row.appointment();
            if (exclusionRepository.existsByOrganisationIdAndAppointmentFhirId(
                    organisationId, a.appointmentId())) {
                continue;
            }
            PolledAppointmentEntity entity = repository
                    .findByOrganisationIdAndAppointmentFhirId(organisationId, a.appointmentId())
                    .orElseGet(PolledAppointmentEntity::new);
            boolean wasVoidedBefore = entity.getId() != null && entity.isVoided();
            applyAppointment(entity, organisationId, a, row.patient(), now);
            repository.save(entity);
            if (entity.isVoided()) {
                cancelledAppointmentNotificationService.handleVoidedAppointment(entity, wasVoidedBefore);
            }
        }
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
