package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.notification.content.AppointmentNotificationContent;
import nl.openmrs.comm_module.notification.content.AppointmentNotificationContentProvider;
import nl.openmrs.comm_module.notification.content.DutchAppointmentReminderBodyFormatter;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderTestSpecs;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderMessageBuilderTest {

    @Mock
    private AppointmentNotificationContentProvider contentProvider;

    private AppointmentReminderMessageBuilder builder;

    @BeforeEach
    void setUp() {
        NotificationSchedulerProperties props = new NotificationSchedulerProperties();
        props.setDefaultProvider(MessagingProviderType.SWIFTSEND);
        props.setReminderZoneId("Europe/Amsterdam");
        builder =
                new AppointmentReminderMessageBuilder(
                        props, contentProvider, new DutchAppointmentReminderBodyFormatter());
    }

    @Test
    void bouwtBerichtMetDatumTijdLocatieEnInstructies() {
        PolledAppointmentEntity appointment = appointment("+31612345678", "Jan de Vries");
        when(contentProvider.resolve(any())).thenReturn(content("poli-2", "Nuchter blijven"));

        var message =
                builder.buildReminder(appointment, AppointmentReminderTestSpecs.HOURS_24).orElseThrow();

        assertEquals("+31612345678", message.getRecipient());
        assertTrue(message.getBody().contains("Jan de Vries"));
        assertTrue(message.getBody().contains("poli-2"));
        assertTrue(message.getBody().contains("Nuchter blijven"));
    }

    @Test
    void leegBijOntbrekendTelefoonnummer() {
        PolledAppointmentEntity appointment = appointment(null, "Jan");
        assertTrue(builder.buildReminder(appointment, AppointmentReminderTestSpecs.HOURS_24).isEmpty());
    }

    private static PolledAppointmentEntity appointment(String phone, String name) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setOrganisationId("org");
        a.setAppointmentUuid("uuid-1");
        a.setAppointmentFhirId("apt-1");
        a.setPatientFhirId("pat-1");
        a.setPatientPhone(phone);
        a.setPatientDisplayName(name);
        a.setAppointmentDatetime(Instant.parse("2026-05-19T14:30:00Z"));
        a.setLastPolledAt(Instant.now());
        return a;
    }

    private static AppointmentNotificationContent content(String location, String instructions) {
        return new AppointmentNotificationContent(
                Instant.parse("2026-05-19T14:30:00Z").atZone(ZoneId.of("Europe/Amsterdam")),
                location,
                instructions);
    }
}
