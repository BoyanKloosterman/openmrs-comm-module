package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelledAppointmentNotificationServiceTest {

    @Mock
    private NotificationDeliveryLogService deliveryLogService;

    @InjectMocks
    private CancelledAppointmentNotificationService service;

    @Test
    void negeertActieveAfspraak() {
        service.handleVoidedAppointment(appointment(false), false);
        verify(deliveryLogService, never()).cancelQueuedNotifications(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void verwijdertQueuedBijAnnulering() {
        PolledAppointmentEntity apt = appointment(true);
        when(deliveryLogService.cancelQueuedNotifications("apt-1")).thenReturn(2);
        when(deliveryLogService.hasAnySuccessfulDelivery("apt-1")).thenReturn(false);

        service.handleVoidedAppointment(apt, false);

        verify(deliveryLogService).cancelQueuedNotifications("apt-1");
    }

    @Test
    void controleertVerstuurdBijOvergangNaarGeannuleerd() {
        PolledAppointmentEntity apt = appointment(true);
        when(deliveryLogService.cancelQueuedNotifications("apt-1")).thenReturn(0);
        when(deliveryLogService.hasAnySuccessfulDelivery("apt-1")).thenReturn(true);

        service.handleVoidedAppointment(apt, false);

        verify(deliveryLogService).hasAnySuccessfulDelivery("apt-1");
    }

    @Test
    void geenVerstuurdCheckAlsAlGeannuleerd() {
        PolledAppointmentEntity apt = appointment(true);
        when(deliveryLogService.cancelQueuedNotifications("apt-1")).thenReturn(0);

        service.handleVoidedAppointment(apt, true);

        verify(deliveryLogService, never()).hasAnySuccessfulDelivery(org.mockito.ArgumentMatchers.any());
    }

    private static PolledAppointmentEntity appointment(boolean voided) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setAppointmentFhirId("apt-1");
        a.setAppointmentDatetime(Instant.parse("2026-05-20T10:00:00Z"));
        a.setVoided(voided);
        return a;
    }
}
