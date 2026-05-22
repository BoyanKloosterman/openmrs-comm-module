package nl.openmrs.comm_module.notification.content;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAppointmentNotificationContentProviderTest {

    @Test
    void resolveertDatumLocatieEnInstructies() {
        NotificationSchedulerProperties props = new NotificationSchedulerProperties();
        props.setReminderZoneId("Europe/Amsterdam");
        props.setDefaultInstructions("Neem legitimatie mee");
        DefaultAppointmentNotificationContentProvider provider =
                new DefaultAppointmentNotificationContentProvider(props, Optional.empty());

        PolledAppointmentEntity entity = new PolledAppointmentEntity();
        entity.setAppointmentDatetime(Instant.parse("2026-05-19T14:30:00Z"));
        entity.setLocationId("Polikliniek cardiologie, kamer 3");
        entity.setAppointmentReason("Nuchter blijven");

        AppointmentNotificationContent content = provider.resolve(entity);

        assertEquals("Polikliniek cardiologie, kamer 3", content.location());
        assertEquals("Nuchter blijven", content.instructions());
        assertTrue(content.hasInstructions());
    }

    @Test
    void valtTerugOpDefaultInstructies() {
        NotificationSchedulerProperties props = new NotificationSchedulerProperties();
        props.setReminderZoneId("UTC");
        props.setDefaultInstructions("Medicijnen meenemen");
        DefaultAppointmentNotificationContentProvider provider =
                new DefaultAppointmentNotificationContentProvider(props, Optional.empty());

        PolledAppointmentEntity entity = new PolledAppointmentEntity();
        entity.setAppointmentDatetime(Instant.parse("2026-05-19T10:00:00Z"));

        AppointmentNotificationContent content = provider.resolve(entity);

        assertEquals(AppointmentNotificationContent.UNKNOWN_LOCATION, content.locationOrDefault());
        assertEquals("Medicijnen meenemen", content.instructions());
    }

    @Test
    void formatterZetDatumTijdLocatieEnInstructiesInTekst() {
        AppointmentNotificationContent content =
                new AppointmentNotificationContent(
                        Instant.parse("2026-05-19T14:30:00Z")
                                .atZone(java.time.ZoneId.of("Europe/Amsterdam")),
                        "Poli 2, kamer 12",
                        "Nuchter blijven");
        DutchAppointmentReminderBodyFormatter formatter = new DutchAppointmentReminderBodyFormatter();

        String body = formatter.formatBody("Beste Jan", "24 uur", content);

        assertTrue(body.contains("Datum:"));
        assertTrue(body.contains("Tijd:"));
        assertTrue(body.contains("Locatie: Poli 2, kamer 12"));
        assertTrue(body.contains("Instructies: Nuchter blijven"));
    }

    @Test
    void geenInstructieregelAlsLeeg() {
        AppointmentNotificationContent content =
                new AppointmentNotificationContent(
                        Instant.parse("2026-05-19T14:30:00Z")
                                .atZone(java.time.ZoneId.of("Europe/Amsterdam")),
                        "Poli",
                        null);
        DutchAppointmentReminderBodyFormatter formatter = new DutchAppointmentReminderBodyFormatter();

        String body = formatter.formatBody("Beste Jan", "1 uur", content);

        assertFalse(body.contains("Instructies:"));
    }
}
