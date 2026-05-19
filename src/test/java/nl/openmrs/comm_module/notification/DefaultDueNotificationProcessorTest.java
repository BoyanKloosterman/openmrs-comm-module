package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDueNotificationProcessorTest {

    @Mock
    private AppointmentReminderQueryService appointmentReminderQueryService;

    @Mock
    private AppointmentReminderPublisher appointmentReminderPublisher;

    @InjectMocks
    private DefaultDueNotificationProcessor processor;

    @Test
    void roeptQueryEnPublisherAan() {
        PolledAppointmentEntity appointment = new PolledAppointmentEntity();
        appointment.setAppointmentFhirId("apt-1");
        appointment.setAppointmentDatetime(Instant.now());
        when(appointmentReminderQueryService.findAppointmentsDueFor24HourReminder())
                .thenReturn(List.of(appointment));
        when(appointmentReminderPublisher.publish24HourReminders(List.of(appointment)))
                .thenReturn(1);

        processor.processDueNotifications();

        verify(appointmentReminderQueryService).findAppointmentsDueFor24HourReminder();
        verify(appointmentReminderPublisher).publish24HourReminders(List.of(appointment));
    }
}
