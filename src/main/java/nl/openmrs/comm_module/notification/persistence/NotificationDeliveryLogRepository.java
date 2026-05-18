package nl.openmrs.comm_module.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLogEntity, Long> {

    boolean existsByEncounterFhirIdAndMessageTypeAndSuccessfulTrue(String encounterFhirId, String messageType);
}
