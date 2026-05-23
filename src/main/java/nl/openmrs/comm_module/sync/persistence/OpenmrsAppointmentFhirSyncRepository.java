package nl.openmrs.comm_module.sync.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpenmrsAppointmentFhirSyncRepository
        extends JpaRepository<OpenmrsAppointmentFhirSyncEntity, Long> {

    Optional<OpenmrsAppointmentFhirSyncEntity> findByOpenmrsAppointmentId(int openmrsAppointmentId);
}
