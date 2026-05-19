package nl.openmrs.comm_module.poll.persistence;

import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.poll.AppointmentPollPersistence;
import nl.openmrs.comm_module.scheduling.NotificationScheduler;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@TestPropertySource(
        properties = {
            "comm.notification.scheduler.enabled=false",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
class JpaAppointmentPollPersistenceTest {

    private static final Instant t = Instant.parse("2026-05-10T09:00:00Z");

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

    @Test
    void slaatAppointmentEnPatientOp() {
        AppointmentPollDto apt = new AppointmentPollDto("uuid-1", "apt-1", "pat-1", t, "loc-1", "consult", false);
        PatientPollDto patient = new PatientPollDto("pat-1", "Jan Jansen", "+31612345678");
        List<AppointmentWithPatientDto> batch = List.of(new AppointmentWithPatientDto(apt, patient));

        persistence.upsertPollResults("org-a", batch);

        List<PolledAppointmentEntity> all = repository.findAll();
        assertEquals(1, all.size());
        PolledAppointmentEntity a = all.get(0);
        assertEquals("org-a", a.getOrganisationId());
        assertEquals("consult", a.getAppointmentType());
        assertEquals("loc-1", a.getLocationId());
        assertEquals("+31612345678", a.getPatientPhone());
    }

    @Test
    void upsertWerktBijTweedePoll() {
        Instant t1 = Instant.parse("2026-05-10T09:00:00Z");
        Instant t2 = Instant.parse("2026-05-11T09:00:00Z");
        AppointmentPollDto first = new AppointmentPollDto("uuid-1", "apt-9", "pat-9", t1, null, "oud", false);
        persistence.upsertPollResults("org-b", List.of(new AppointmentWithPatientDto(first, null)));

        PatientPollDto patient = new PatientPollDto("pat-9", "P", "+31111");
        AppointmentPollDto second = new AppointmentPollDto("uuid-1", "apt-9", "pat-9", t2, "loc-x", "nieuw", true);
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
}
