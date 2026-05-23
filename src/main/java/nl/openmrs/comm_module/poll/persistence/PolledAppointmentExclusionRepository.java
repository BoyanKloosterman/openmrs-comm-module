package nl.openmrs.comm_module.poll.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolledAppointmentExclusionRepository
        extends JpaRepository<PolledAppointmentExclusionEntity, Long> {

    boolean existsByOrganisationIdAndAppointmentFhirId(String organisationId, String appointmentFhirId);
}
