package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentReminderMessageBuilderTest {

    private AppointmentReminderMessageBuilder builder;

    @BeforeEach
    void setUp() {
        NotificationSchedulerProperties props = new NotificationSchedulerProperties();
        props.setDefaultProvider(MessagingProviderType.SWIFTSEND);
        props.setDefaultInstructions("Neem legitimatie mee.");
        props.setReminderZoneId("Europe/Amsterdam");
        builder = new AppointmentReminderMessageBuilder(props);
    }

    @Test
    void bouwtBerichtMetDatumTijdLocatieEnInstructies() {
        PolledEncounterEntity encounter = encounter(
                "+31600112233",
                "Jan Jansen",
                Instant.parse("2026-06-15T08:00:00Z"),
                "poli-3",
                "Controle");

        NotificationQueueMessage message =
                builder.build24HourReminder(encounter).orElseThrow();

        assertEquals("+31600112233", message.getRecipient());
        assertEquals(MessagingProviderType.SWIFTSEND, message.getProvider());
        assertEquals(AppointmentReminderMessageBuilder.MESSAGE_TYPE_24H, message.getMessageType());
        assertTrue(message.getBody().contains("Beste Jan Jansen"));
        assertTrue(message.getBody().contains("Datum:"));
        assertTrue(message.getBody().contains("Tijd:"));
        assertTrue(message.getBody().contains("Locatie: poli-3"));
        assertTrue(message.getBody().contains("Instructies: Controle. Neem legitimatie mee."));
        // 08:00 UTC = 10:00 Amsterdam (CEST)
        assertTrue(message.getBody().contains("10:00"));
    }

    @Test
    void leegAlsGeenTelefoon() {
        PolledEncounterEntity encounter = encounter(null, "Jan", Instant.now(), "loc", null);
        assertTrue(builder.build24HourReminder(encounter).isEmpty());
    }

    private static PolledEncounterEntity encounter(
            String phone, String name, Instant when, String locationId, String type) {
        PolledEncounterEntity e = new PolledEncounterEntity();
        e.setOrganisationId("org");
        e.setEncounterUuid("uuid-1");
        e.setEncounterFhirId("enc-1");
        e.setPatientFhirId("pat-1");
        e.setPatientPhone(phone);
        e.setPatientDisplayName(name);
        e.setEncounterDatetime(when);
        e.setLocationId(locationId);
        e.setEncounterType(type);
        e.setVoided(false);
        e.setLastPolledAt(Instant.parse("2026-05-01T00:00:00Z"));
        return e;
    }
}
