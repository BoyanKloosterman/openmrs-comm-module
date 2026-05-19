package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
import nl.openmrs.comm_module.scheduling.NotificationScheduler;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AppointmentReminderQueryServiceTest {

    private static final String ORG = "test-org";
    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");

    @MockitoBean
    @SuppressWarnings("unused")
    private OpenmrsFhirPollingService openmrsFhirPollingService;

    @MockitoBean
    @SuppressWarnings("unused")
    private NotificationScheduler notificationScheduler;

    @MockitoBean
    private Clock clock;

    @Autowired
    private PolledAppointmentRepository repository;

    @Autowired
    private AppointmentReminderQueryService queryService;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void vindtAppointmentIn24uVenster() {
        save(ORG, "apt-in", Instant.parse("2026-05-19T10:05:00Z"), false);
        save(ORG, "apt-early", Instant.parse("2026-05-19T08:00:00Z"), false);
        save(ORG, "apt-late", Instant.parse("2026-05-20T10:00:00Z"), false);
        save(ORG, "apt-past", Instant.parse("2026-05-17T10:00:00Z"), false);
        save(ORG, "apt-voided", Instant.parse("2026-05-19T10:10:00Z"), true);
        save("other-org", "apt-wrong", Instant.parse("2026-05-19T10:10:00Z"), false);

        List<PolledAppointmentEntity> due = queryService.findAppointmentsDueFor24HourReminder();

        assertEquals(1, due.size());
        assertEquals("apt-in", due.get(0).getAppointmentFhirId());
    }

    @Test
    void legeLijstAlsGeenMatches() {
        save(ORG, "apt-far", Instant.parse("2026-05-22T12:00:00Z"), false);
        assertTrue(queryService.findAppointmentsDueFor24HourReminder().isEmpty());
    }

    @Test
    void appointmentOpVensterGrensWordtGevonden() {
        save(ORG, "apt-grens-start", Instant.parse("2026-05-19T09:30:00Z"), false);
        save(ORG, "apt-grens-einde", Instant.parse("2026-05-19T10:29:59Z"), false);
        save(ORG, "apt-buiten", Instant.parse("2026-05-19T10:30:00Z"), false);

        List<PolledAppointmentEntity> due = queryService.findAppointmentsDueFor24HourReminder();

        assertEquals(2, due.size());
    }

    @Test
    void sluitBegonnenAfspraakInVensterUit() {
        save(ORG, "apt-started", Instant.parse("2026-05-18T09:30:00Z"), false);
        save(ORG, "apt-in", Instant.parse("2026-05-19T10:05:00Z"), false);

        List<PolledAppointmentEntity> due = queryService.findAppointmentsDueFor24HourReminder();

        assertEquals(1, due.size());
        assertEquals("apt-in", due.get(0).getAppointmentFhirId());
    }

    private void save(String organisationId, String appointmentFhirId, Instant appointmentDatetime, boolean voided) {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setOrganisationId(organisationId);
        a.setAppointmentUuid("uuid-" + appointmentFhirId);
        a.setAppointmentFhirId(appointmentFhirId);
        a.setPatientFhirId("pat-" + appointmentFhirId);
        a.setAppointmentDatetime(appointmentDatetime);
        a.setVoided(voided);
        a.setLastPolledAt(NOW);
        repository.save(a);
    }
}
