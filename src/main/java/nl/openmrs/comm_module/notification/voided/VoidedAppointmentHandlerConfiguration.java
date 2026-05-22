package nl.openmrs.comm_module.notification.voided;

import nl.openmrs.comm_module.notification.CancelledAppointmentNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class VoidedAppointmentHandlerConfiguration {

    @Bean
    List<VoidedAppointmentHandler> voidedAppointmentHandlers(
            CancelledAppointmentNotificationService cancelledAppointmentNotificationService) {
        return List.of(cancelledAppointmentNotificationService);
    }
}
