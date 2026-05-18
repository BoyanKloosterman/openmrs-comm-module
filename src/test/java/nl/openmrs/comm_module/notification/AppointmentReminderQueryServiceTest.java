package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.poll.persistence.PolledEncounterEntity;
import nl.openmrs.comm_module.poll.persistence.PolledEncounterRepository;
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
    private PolledEncounterRepository repository;

    @Autowired
    private AppointmentReminderQueryService queryService;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void vindtEncounterIn24uVenster() {
        save(ORG, "enc-in", Instant.parse("2026-05-19T10:05:00Z"), false);
        save(ORG, "enc-early", Instant.parse("2026-05-19T08:00:00Z"), false);
        save(ORG, "enc-late", Instant.parse("2026-05-20T10:00:00Z"), false);
        save(ORG, "enc-past", Instant.parse("2026-05-17T10:00:00Z"), false);
        save(ORG, "enc-voided", Instant.parse("2026-05-19T10:10:00Z"), true);
        save("other-org", "enc-wrong", Instant.parse("2026-05-19T10:10:00Z"), false);

        List<PolledEncounterEntity> due = queryService.findEncountersDueFor24HourReminder();

        assertEquals(1, due.size());
        assertEquals("enc-in", due.get(0).getEncounterFhirId());
    }

    @Test
    void legeLijstAlsGeenMatches() {
        save(ORG, "enc-far", Instant.parse("2026-05-22T12:00:00Z"), false);
        assertTrue(queryService.findEncountersDueFor24HourReminder().isEmpty());
    }

    @Test
    void encounterOpVensterGrensWordtGevonden() {
        // target = 19 mei 10:00; venster ±30 min → 09:30 t/m 10:30 exclusief einde
        save(ORG, "enc-grens-start", Instant.parse("2026-05-19T09:30:00Z"), false);
        save(ORG, "enc-grens-einde", Instant.parse("2026-05-19T10:29:59Z"), false);
        save(ORG, "enc-buiten", Instant.parse("2026-05-19T10:30:00Z"), false);

        List<PolledEncounterEntity> due = queryService.findEncountersDueFor24HourReminder();

        assertEquals(2, due.size());
    }

    @Test
    void sluitBegonnenAfspraakInVensterUit() {
        // In DB-venster maar starttijd al verstreken t.o.v. NOW (US-001-4)
        save(ORG, "enc-started", Instant.parse("2026-05-18T09:30:00Z"), false);
        save(ORG, "enc-in", Instant.parse("2026-05-19T10:05:00Z"), false);

        List<PolledEncounterEntity> due = queryService.findEncountersDueFor24HourReminder();

        assertEquals(1, due.size());
        assertEquals("enc-in", due.get(0).getEncounterFhirId());
    }

    private void save(String organisationId, String encounterFhirId, Instant encounterDatetime, boolean voided) {
        PolledEncounterEntity e = new PolledEncounterEntity();
        e.setOrganisationId(organisationId);
        e.setEncounterUuid("uuid-" + encounterFhirId);
        e.setEncounterFhirId(encounterFhirId);
        e.setPatientFhirId("pat-" + encounterFhirId);
        e.setEncounterDatetime(encounterDatetime);
        e.setVoided(voided);
        e.setLastPolledAt(NOW);
        repository.save(e);
    }
}
