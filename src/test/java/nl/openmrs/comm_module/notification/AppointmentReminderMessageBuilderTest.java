package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentReminderMessageBuilderTest {

    private AppointmentReminderMessageBuilder builder;

    @BeforeEach
    void setUp() {
        NotificationSchedulerProperties props = new NotificationSchedulerProperties();
        props.setDefaultProvider(MessagingProviderType.SWIFTSEND);
        props.setReminderZoneId("Europe/Amsterdam");
        builder = new AppointmentReminderMessageBuilder(props);
    }

    @Test
    void bouwtBerichtMetDatumTijdEnLocatie() {
        PolledAppointmentEntity appointment = appointment(
                "+31612345678", "Jan de Vries", Instant.parse("2026-05-19T14:30:00Z"), "poli-2", "Controle");

        var message =
                builder.build24HourReminder(appointment).orElseThrow();

        assertEquals("+31612345678", message.getRecipient());
        assertEquals(AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H, message.getMessageType());
        assertEquals("apt-1", message.getAppointmentFhirId());
        assertTrue(message.getBody().contains("Jan de Vries"));
        assertTrue(message.getBody().contains("poli-2"));
    }

    @Test
    void leegBijOntbrekendTelefoonnummer() {
        PolledAppointmentEntity appointment = appointment(null, "Jan", Instant.now(), "loc", null);
        assertTrue(builder.build24HourReminder(appointment).isEmpty());
    }

    private static PolledAppointmentEntity appointment(
            String phone, String name, Instant when, String location, String type) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setOrganisationId("org");
        a.setAppointmentUuid("uuid-1");
        a.setAppointmentFhirId("apt-1");
        a.setPatientFhirId("pat-1");
        a.setPatientPhone(phone);
        a.setPatientDisplayName(name);
        a.setAppointmentDatetime(when);
        a.setLocationId(location);
        a.setAppointmentType(type);
        a.setLastPolledAt(Instant.now());
        return a;
    }
}
