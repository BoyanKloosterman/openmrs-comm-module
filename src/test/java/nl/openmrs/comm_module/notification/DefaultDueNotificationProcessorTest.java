package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.notification.reminder.AppointmentReminderTestSpecs;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDueNotificationProcessorTest {

    @Mock
    private AppointmentReminderQueryService appointmentReminderQueryService;

    @Mock
    private AppointmentReminderPublisher appointmentReminderPublisher;

    @Test
    void roeptQueryEnPublisherAanVoorElkeSpec() {
        PolledAppointmentEntity appointment = new PolledAppointmentEntity();
        appointment.setAppointmentFhirId("apt-1");
        appointment.setAppointmentDatetime(Instant.now());

        DefaultDueNotificationProcessor processorWithSpecs =
                new DefaultDueNotificationProcessor(
                        List.of(
                                AppointmentReminderTestSpecs.HOURS_24,
                                AppointmentReminderTestSpecs.HOURS_1),
                        appointmentReminderQueryService,
                        appointmentReminderPublisher);

        when(appointmentReminderQueryService.findAppointmentsDueFor(AppointmentReminderTestSpecs.HOURS_24))
                .thenReturn(List.of(appointment));
        when(appointmentReminderQueryService.findAppointmentsDueFor(AppointmentReminderTestSpecs.HOURS_1))
                .thenReturn(List.of());
        when(appointmentReminderPublisher.publishReminders(
                        List.of(appointment), AppointmentReminderTestSpecs.HOURS_24))
                .thenReturn(1);

        processorWithSpecs.processDueNotifications();

        verify(appointmentReminderQueryService).findAppointmentsDueFor(AppointmentReminderTestSpecs.HOURS_24);
        verify(appointmentReminderQueryService).findAppointmentsDueFor(AppointmentReminderTestSpecs.HOURS_1);
        verify(appointmentReminderPublisher)
                .publishReminders(List.of(appointment), AppointmentReminderTestSpecs.HOURS_24);
    }
}
