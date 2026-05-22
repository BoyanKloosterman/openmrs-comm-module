package nl.openmrs.comm_module.poll.persistence;

import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import nl.openmrs.comm_module.scheduling.NotificationScheduler;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@TestPropertySource(
        properties = {
            "comm.notification.scheduler.enabled=false",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
class JpaAppointmentPollPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-05-20T12:00:00Z");
    private static final Instant PAST = Instant.parse("2026-05-19T10:00:00Z");
    private static final Instant FUTURE = Instant.parse("2026-05-21T10:00:00Z");

    @MockitoBean
    private Clock clock;

    @MockitoBean
    @SuppressWarnings("unused")
    private OpenmrsFhirPollingService openmrsFhirPollingService;

    @MockitoBean
    @SuppressWarnings("unused")
    private NotificationScheduler notificationScheduler;

    @Autowired
    private PolledAppointmentRepository repository;

    @Autowired
    private AppointmentPollPersistence persistence;

    @Autowired
    private PolledAppointmentExclusionRepository exclusionRepository;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void slaatAppointmentEnPatientOp() {
        AppointmentPollDto apt =
                new AppointmentPollDto("uuid-1", "apt-1", "pat-1", FUTURE, "loc-1", "consult", "Nuchter blijven", false);
        PatientPollDto patient = new PatientPollDto("pat-1", "Jan Jansen", "+31612345678");
        List<AppointmentWithPatientDto> batch = List.of(new AppointmentWithPatientDto(apt, patient));

        persistence.upsertPollResults("org-a", batch);

        List<PolledAppointmentEntity> all = repository.findAll();
        assertEquals(1, all.size());
        PolledAppointmentEntity a = all.get(0);
        assertEquals("org-a", a.getOrganisationId());
        assertEquals("consult", a.getAppointmentType());
        assertEquals("Nuchter blijven", a.getAppointmentReason());
        assertEquals("loc-1", a.getLocationId());
        assertEquals("+31612345678", a.getPatientPhone());
    }

    @Test
    void verledenAfspraakWordtUitgeslotenZonderOpslag() {
        AppointmentPollDto past =
                new AppointmentPollDto("uuid-p", "apt-past", "pat-1", PAST, "loc", "consult", null, false);
        persistence.upsertPollResults("org-past", List.of(new AppointmentWithPatientDto(past, null)));

        assertEquals(0, repository.count());
        assertTrue(exclusionRepository.existsByOrganisationIdAndAppointmentFhirId("org-past", "apt-past"));
    }

    @Test
    void tweedePollMetVerledenAfspraakWerktNietOpnieuwBij() {
        AppointmentPollDto past =
                new AppointmentPollDto("uuid-p2", "apt-past-2", "pat-1", PAST, "loc", "type", null, false);
        persistence.upsertPollResults("org-p2", List.of(new AppointmentWithPatientDto(past, null)));
        AppointmentPollDto gewijzigd =
                new AppointmentPollDto("uuid-p2", "apt-past-2", "pat-1", PAST, "loc-nieuw", "nieuw-type", "x", false);
        persistence.upsertPollResults("org-p2", List.of(new AppointmentWithPatientDto(gewijzigd, null)));

        assertEquals(0, repository.count());
        assertEquals(1, exclusionRepository.count());
    }

    @Test
    void verledenRijInDbWordtBijPollUitgesloten() {
        AppointmentPollDto future =
                new AppointmentPollDto("uuid-f", "apt-age", "pat-1", FUTURE, "loc", "consult", null, false);
        persistence.upsertPollResults("org-age", List.of(new AppointmentWithPatientDto(future, null)));
        assertEquals(1, repository.count());

        when(clock.instant()).thenReturn(Instant.parse("2026-05-22T12:00:00Z"));
        persistence.upsertPollResults("org-age", List.of());

        assertTrue(exclusionRepository.existsByOrganisationIdAndAppointmentFhirId("org-age", "apt-age"));
        assertEquals(1, repository.count());
    }

    @Test
    void upsertWerktBijTweedePoll() {
        Instant t1 = Instant.parse("2026-05-21T09:00:00Z");
        Instant t2 = Instant.parse("2026-05-22T09:00:00Z");
        AppointmentPollDto first = new AppointmentPollDto("uuid-1", "apt-9", "pat-9", t1, null, "oud", null, false);
        persistence.upsertPollResults("org-b", List.of(new AppointmentWithPatientDto(first, null)));

        PatientPollDto patient = new PatientPollDto("pat-9", "P", "+31111");
        AppointmentPollDto second = new AppointmentPollDto("uuid-1", "apt-9", "pat-9", t2, "loc-x", "nieuw", "Reden gewijzigd", true);
        persistence.upsertPollResults("org-b", List.of(new AppointmentWithPatientDto(second, patient)));

        Optional<PolledAppointmentEntity> stored =
                repository.findByOrganisationIdAndAppointmentFhirId("org-b", "apt-9");
        assertTrue(stored.isPresent());
        assertEquals("nieuw", stored.get().getAppointmentType());
        assertEquals("loc-x", stored.get().getLocationId());
        assertTrue(stored.get().isVoided());
        assertEquals(t2, stored.get().getAppointmentDatetime());
        assertEquals(1, repository.count());
    }

    @Test
    void upsertSlaatUitgeslotenFhirIdOver() {
        PolledAppointmentExclusionEntity exclusion = new PolledAppointmentExclusionEntity();
        exclusion.setOrganisationId("org-x");
        exclusion.setAppointmentFhirId("apt-skip");
        exclusion.setExcludedAt(PAST);
        exclusionRepository.save(exclusion);

        AppointmentPollDto apt =
                new AppointmentPollDto("uuid-s", "apt-skip", "pat-1", FUTURE, "loc", "type", null, false);
        persistence.upsertPollResults("org-x", List.of(new AppointmentWithPatientDto(apt, null)));

        assertEquals(0, repository.count());
    }
}
