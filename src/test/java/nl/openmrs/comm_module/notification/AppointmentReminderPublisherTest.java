package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
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
        PolledEncounterEntity withPhone = encounter("enc-a");
        PolledEncounterEntity noPhone = encounter("enc-b");
        NotificationQueueMessage msg = new NotificationQueueMessage();
        msg.setNotificationId(java.util.UUID.randomUUID());

        when(eligibilityService.maySend24HourReminder(withPhone)).thenReturn(true);
        when(eligibilityService.maySend24HourReminder(noPhone)).thenReturn(true);
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
        PolledEncounterEntity started = encounter("enc-started");
        when(eligibilityService.maySend24HourReminder(started)).thenReturn(false);

        assertEquals(0, publisher.publish24HourReminders(List.of(started)));

        verify(messageBuilder, never()).build24HourReminder(any());
        verify(rabbitMqProducer, never()).publish(any());
        verify(deliveryLogService, never()).recordQueued(any());
    }

    @Test
    void slaatOverBijEerderSuccesvolVerstuurd() {
        PolledEncounterEntity enc = encounter("enc-done");
        when(eligibilityService.maySend24HourReminder(enc)).thenReturn(true);
        when(deliveryLogService.hasSuccessfulDelivery("enc-done", AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H))
                .thenReturn(true);

        assertEquals(0, publisher.publish24HourReminders(List.of(enc)));

        verify(messageBuilder, never()).build24HourReminder(any());
        verify(rabbitMqProducer, never()).publish(any());
    }

    @Test
    void publiceertNietsBijLegeLijst() {
        assertEquals(0, publisher.publish24HourReminders(List.of()));
        verify(rabbitMqProducer, never()).publish(any());
    }

    private static PolledEncounterEntity encounter(String fhirId) {
        PolledEncounterEntity e = new PolledEncounterEntity();
        e.setEncounterFhirId(fhirId);
        e.setEncounterUuid("uuid-" + fhirId);
        e.setOrganisationId("org");
        e.setPatientFhirId("pat");
        e.setEncounterDatetime(Instant.now());
        e.setLastPolledAt(Instant.now());
        return e;
    }
}
