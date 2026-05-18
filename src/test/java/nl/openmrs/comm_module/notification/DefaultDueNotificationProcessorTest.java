package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
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
        PolledEncounterEntity encounter = new PolledEncounterEntity();
        encounter.setEncounterFhirId("enc-1");
        encounter.setEncounterDatetime(Instant.now());
        when(appointmentReminderQueryService.findEncountersDueFor24HourReminder())
                .thenReturn(List.of(encounter));
        when(appointmentReminderPublisher.publish24HourReminders(List.of(encounter)))
                .thenReturn(1);

        processor.processDueNotifications();

        verify(appointmentReminderQueryService).findEncountersDueFor24HourReminder();
        verify(appointmentReminderPublisher).publish24HourReminders(List.of(encounter));
    }
}
