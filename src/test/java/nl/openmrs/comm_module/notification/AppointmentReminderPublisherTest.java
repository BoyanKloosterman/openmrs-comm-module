package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderPublisherTest {

    @Mock
    private AppointmentReminderMessageBuilder messageBuilder;

    @Mock
    private AppointmentReminderEligibilityService eligibilityService;

    @Mock
    private NotificationDeliveryLogService deliveryLogService;

    @Mock
    private RabbitMqProducer rabbitMqProducer;

    @InjectMocks
    private AppointmentReminderPublisher publisher;

    @Test
    void publiceertAlleenGeldigeBerichten() {
        PolledAppointmentEntity withPhone = appointment("apt-a");
        PolledAppointmentEntity noPhone = appointment("apt-b");
        NotificationQueueMessage msg = new NotificationQueueMessage();
        msg.setNotificationId(java.util.UUID.randomUUID());

        when(eligibilityService.maySendReminder(withPhone)).thenReturn(true);
        when(eligibilityService.maySendReminder(noPhone)).thenReturn(true);
        when(deliveryLogService.hasSuccessfulDelivery(any(), any())).thenReturn(false);
        when(messageBuilder.build24HourReminder(withPhone)).thenReturn(Optional.of(msg));
        when(messageBuilder.build24HourReminder(noPhone)).thenReturn(Optional.empty());

        int queued = publisher.publish24HourReminders(List.of(withPhone, noPhone));

        assertEquals(1, queued);
        verify(rabbitMqProducer).publish(msg);
        verify(deliveryLogService).recordQueued(msg);
    }

    @Test
    void slaatBegonnenAfspraakOver() {
        PolledAppointmentEntity started = appointment("apt-started");
        when(eligibilityService.maySendReminder(started)).thenReturn(false);

        assertEquals(0, publisher.publish24HourReminders(List.of(started)));

        verify(messageBuilder, never()).build24HourReminder(any());
        verify(rabbitMqProducer, never()).publish(any());
        verify(deliveryLogService, never()).recordQueued(any());
    }

    @Test
    void slaatOverBijEerderSuccesvolVerstuurd() {
        PolledAppointmentEntity apt = appointment("apt-done");
        when(eligibilityService.maySendReminder(apt)).thenReturn(true);
        when(deliveryLogService.hasSuccessfulDelivery("apt-done", AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H))
                .thenReturn(true);

        assertEquals(0, publisher.publish24HourReminders(List.of(apt)));

        verify(messageBuilder, never()).build24HourReminder(any());
        verify(rabbitMqProducer, never()).publish(any());
    }

    @Test
    void publiceertNietsBijLegeLijst() {
        assertEquals(0, publisher.publish24HourReminders(List.of()));
        verify(rabbitMqProducer, never()).publish(any());
    }

    @Test
    void publiceert1uOokAls24uAlVerstuurd() {
        PolledAppointmentEntity apt = appointment("apt-1h");
        NotificationQueueMessage msg = new NotificationQueueMessage();
        msg.setNotificationId(java.util.UUID.randomUUID());

        when(eligibilityService.maySendReminder(apt)).thenReturn(true);
        when(deliveryLogService.hasSuccessfulDelivery("apt-1h", AppointmentReminderMessageBuilder.MESSAGE_TYPE_1H))
                .thenReturn(false);
        when(messageBuilder.build1HourReminder(apt)).thenReturn(Optional.of(msg));

        assertEquals(1, publisher.publish1HourReminders(List.of(apt)));
        verify(rabbitMqProducer).publish(msg);
    }

    private static PolledAppointmentEntity appointment(String fhirId) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setAppointmentFhirId(fhirId);
        a.setAppointmentUuid("uuid-" + fhirId);
        a.setOrganisationId("org");
        a.setPatientFhirId("pat");
        a.setAppointmentDatetime(Instant.now());
        a.setLastPolledAt(Instant.now());
        return a;
    }
}
