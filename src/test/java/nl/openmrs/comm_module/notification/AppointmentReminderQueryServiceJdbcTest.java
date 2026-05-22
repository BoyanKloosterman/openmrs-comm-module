package nl.openmrs.comm_module.notification;

import nl.openmrs.comm_module.notification.reminder.AppointmentReminderTestSpecs;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Scheduler-query met JDBC-poll zonder openmrs.fhir.server-url (reference distro). */
@SpringBootTest
@TestPropertySource(
        properties = {
            "openmrs.fhir.server-url=",
            "openmrs.fhir.organisation-id=default",
            "openmrs.fhir.poll-mode=jdbc",
            "openmrs.datasource.url=jdbc:mariadb://127.0.0.1:3307/openmrs",
            "openmrs.datasource.username=openmrs",
            "openmrs.datasource.password=openmrs"
        })
@Transactional
class AppointmentReminderQueryServiceJdbcTest {

    private static final String ORG = "default";
    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");

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
    void vindtAppointmentOokZonderFhirServerUrl() {
        PolledAppointmentEntity a = new PolledAppointmentEntity();
        a.setOrganisationId(ORG);
        a.setAppointmentUuid("uuid-1");
        a.setAppointmentFhirId("apt-jdbc");
        a.setPatientFhirId("pat-1");
        a.setAppointmentDatetime(Instant.parse("2026-05-19T10:05:00Z"));
        a.setVoided(false);
        a.setLastPolledAt(NOW);
        repository.save(a);

        List<PolledAppointmentEntity> due =
                queryService.findAppointmentsDueFor(AppointmentReminderTestSpecs.HOURS_24);

        assertEquals(1, due.size());
        assertEquals("apt-jdbc", due.get(0).getAppointmentFhirId());
    }
}
