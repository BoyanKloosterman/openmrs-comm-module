package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.fhir.OpenmrsFhirOperations;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
import nl.openmrs.comm_module.notification.reminder.AppointmentReminderTestSpecs;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.hl7.fhir.r5.model.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderMessageBuilderTest {

    @Mock
    private OpenmrsFhirOperations fhirOperations;

    private AppointmentReminderMessageBuilder builder;

    @BeforeEach
    void setUp() {
        NotificationSchedulerProperties props = new NotificationSchedulerProperties();
        props.setDefaultProvider(MessagingProviderType.SWIFTSEND);
        props.setReminderZoneId("Europe/Amsterdam");
        builder = new AppointmentReminderMessageBuilder(props, fhirOperations);
    }

    @Test
    void bouwtBerichtMetDatumTijdEnLocatie() {
        when(fhirOperations.readAppointmentByLogicalId(eq("apt-1"))).thenReturn(Optional.empty());

        PolledAppointmentEntity appointment = appointment(
                "+31612345678", "Jan de Vries", Instant.parse("2026-05-19T14:30:00Z"), "poli-2", "Controle");

        var message =
                builder.buildReminder(appointment, AppointmentReminderTestSpecs.HOURS_24).orElseThrow();

        assertEquals("+31612345678", message.getRecipient());
        assertEquals(AppointmentReminderTestSpecs.HOURS_24.messageType(), message.getMessageType());
        assertEquals("apt-1", message.getAppointmentFhirId());
        assertTrue(message.getBody().contains("Jan de Vries"));
        assertTrue(message.getBody().contains("poli-2"));
    }

    @Test
    void bouwt1uBerichtMetZelfdeVelden() {
        when(fhirOperations.readAppointmentByLogicalId(eq("apt-1"))).thenReturn(Optional.empty());
        PolledAppointmentEntity appointment = appointment(
                "+31612345678", "Jan de Vries", Instant.parse("2026-05-19T14:30:00Z"), "poli-2", "Controle");

        var message =
                builder.buildReminder(appointment, AppointmentReminderTestSpecs.HOURS_1).orElseThrow();

        assertEquals(AppointmentReminderTestSpecs.HOURS_1.messageType(), message.getMessageType());
        assertTrue(message.getSubject().contains("1 uur"));
        assertTrue(message.getBody().contains("over 1 uur"));
        assertTrue(message.getBody().contains("poli-2"));
    }

    @Test
    void leegBijOntbrekendTelefoonnummer() {
        PolledAppointmentEntity appointment = appointment(null, "Jan", Instant.now(), "loc", null);
        assertTrue(builder.buildReminder(appointment, AppointmentReminderTestSpecs.HOURS_24).isEmpty());
    }

    @Test
    void gebruiktReasonUitPolledAppointment() {
        PolledAppointmentEntity appointment = appointment(
                "+31612345678", "Jan", Instant.parse("2026-05-19T14:30:00Z"), "Poli 2", "Consult");
        appointment.setAppointmentReason("Nuchter blijven");

        String body =
                builder.buildReminder(appointment, AppointmentReminderTestSpecs.HOURS_24)
                        .orElseThrow()
                        .getBody();

        assertTrue(body.contains("Nuchter blijven"));
        assertTrue(!body.contains("Standaard ziekenhuisregel"));
    }

    @Test
    void gebruiktOpenmrsReasonUitFhirInPlaatsVanDefault() {
        NotificationSchedulerProperties props = new NotificationSchedulerProperties();
        props.setDefaultInstructions("Standaard ziekenhuisregel");
        props.setReminderZoneId("Europe/Amsterdam");
        AppointmentReminderMessageBuilder customBuilder = new AppointmentReminderMessageBuilder(props, fhirOperations);

        Appointment fhirAppt = new Appointment();
        fhirAppt.setId("apt-1");
        OpenmrsFhirAppointmentMetadata.applyTo(fhirAppt, "Poli 2", "Nuchter blijven");
        when(fhirOperations.readAppointmentByLogicalId("apt-1")).thenReturn(Optional.of(fhirAppt));

        PolledAppointmentEntity appointment = appointment(
                "+31612345678", "Jan", Instant.parse("2026-05-19T14:30:00Z"), "Poli 2", "Consult");

        String body =
                customBuilder
                        .buildReminder(appointment, AppointmentReminderTestSpecs.HOURS_24)
                        .orElseThrow()
                        .getBody();

        assertTrue(body.contains("Nuchter blijven"));
        assertTrue(body.contains("Poli 2"));
        assertTrue(!body.contains("Standaard ziekenhuisregel"));
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
