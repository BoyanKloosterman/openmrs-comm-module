package nl.openmrs.comm_module.poll;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentExclusionEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentExclusionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** US-003: verleden afspraken niet opnieuw verwerken bij volgende FHIR-polls. */
@Service
public class AppointmentPollExclusionService {

    private final PolledAppointmentExclusionRepository exclusionRepository;
    private final Clock clock;

    public AppointmentPollExclusionService(
            PolledAppointmentExclusionRepository exclusionRepository, Clock clock) {
        this.exclusionRepository = exclusionRepository;
        this.clock = clock;
    }

    @Transactional
    public void excludeIfAbsent(String organisationId, String appointmentFhirId) {
        excludeIfAbsent(organisationId, appointmentFhirId, clock.instant());
    }

    @Transactional
    public void excludeIfAbsent(String organisationId, String appointmentFhirId, Instant excludedAt) {
        if (organisationId == null
                || organisationId.isBlank()
                || appointmentFhirId == null
                || appointmentFhirId.isBlank()) {
            return;
        }
        if (exclusionRepository.existsByOrganisationIdAndAppointmentFhirId(organisationId, appointmentFhirId)) {
            return;
        }
        PolledAppointmentExclusionEntity exclusion = new PolledAppointmentExclusionEntity();
        exclusion.setOrganisationId(organisationId);
        exclusion.setAppointmentFhirId(appointmentFhirId);
        exclusion.setExcludedAt(excludedAt);
        exclusionRepository.save(exclusion);
    }
}
