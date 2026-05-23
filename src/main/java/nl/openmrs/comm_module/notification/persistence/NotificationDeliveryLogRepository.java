package nl.openmrs.comm_module.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLogEntity, Long> {

    boolean existsByAppointmentFhirIdAndMessageTypeAndSuccessfulTrue(String appointmentFhirId, String messageType);

    boolean existsByAppointmentFhirIdAndSuccessfulTrue(String appointmentFhirId);

    List<NotificationDeliveryLogEntity> findByAppointmentFhirIdAndStatus(
            String appointmentFhirId, String status);

    boolean existsByNotificationIdAndStatus(UUID notificationId, String status);

    Optional<NotificationDeliveryLogEntity> findFirstByAppointmentFhirIdOrderByAttemptedAtDesc(
            String appointmentFhirId);
}
