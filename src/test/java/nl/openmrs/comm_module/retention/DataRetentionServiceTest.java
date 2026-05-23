package nl.openmrs.comm_module.retention;

import nl.openmrs.comm_module.poll.persistence.PolledAppointmentEntity;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentExclusionRepository;
import nl.openmrs.comm_module.poll.persistence.PolledAppointmentRepository;
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
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@TestPropertySource(
        properties = {
            "comm.data-retention.enabled=false",
            "spring.rabbitmq.listener.simple.auto-startup=false"
        })
class DataRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-20T10:00:00Z");

    @MockitoBean
    @SuppressWarnings("unused")
    private OpenmrsFhirPollingService openmrsFhirPollingService;

    @MockitoBean
    @SuppressWarnings("unused")
    private NotificationScheduler notificationScheduler;

    @MockitoBean
    private Clock clock;

    @Autowired
    private DataRetentionService dataRetentionService;

    @Autowired
    private PolledAppointmentRepository polledAppointmentRepository;

    @Autowired
    private PolledAppointmentExclusionRepository exclusionRepository;

    @BeforeEach
    void fixedClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void redacteertPersoonsgegevensEnMaaktExclusion() {
        saveAppointment("apt-old", NOW.minus(15, ChronoUnit.DAYS), "Jan", "+31611112222", "pat-1");
        saveAppointment("apt-new", NOW.minus(5, ChronoUnit.DAYS), "Piet", "+31633334444", "pat-2");

        DataRetentionResult result = dataRetentionService.runCleanup();

        PolledAppointmentEntity old = polledAppointmentRepository
                .findByOrganisationIdAndAppointmentFhirId("org", "apt-old")
                .orElseThrow();
        assertNull(old.getPatientDisplayName());
        assertNull(old.getPatientPhone());
        assertEquals("redacted", old.getPatientFhirId());
        assertTrue(exclusionRepository.existsByOrganisationIdAndAppointmentFhirId("org", "apt-old"));
        assertEquals(1, result.redactedCount());

        PolledAppointmentEntity fresh = polledAppointmentRepository
                .findByOrganisationIdAndAppointmentFhirId("org", "apt-new")
                .orElseThrow();
        assertEquals("Piet", fresh.getPatientDisplayName());
        assertEquals("+31633334444", fresh.getPatientPhone());
        assertEquals("pat-2", fresh.getPatientFhirId());
    }

    @Test
    void verwijdertMetaDataOuderDanJaar() {
        saveAppointment("apt-very-old", NOW.minus(370, ChronoUnit.DAYS), "Oud", "+31600001111", "pat-old");

        DataRetentionResult result = dataRetentionService.runCleanup();

        assertTrue(
                polledAppointmentRepository
                        .findByOrganisationIdAndAppointmentFhirId("org", "apt-very-old")
                        .isEmpty());
        assertEquals(1, result.deletedCount());
    }

    private void saveAppointment(
            String appointmentFhirId, Instant appointmentDatetime, String name, String phone, String patientId) {
        PolledAppointmentEntity entity = new PolledAppointmentEntity();
        entity.setOrganisationId("org");
        entity.setAppointmentUuid("uuid-" + appointmentFhirId);
        entity.setAppointmentFhirId(appointmentFhirId);
        entity.setPatientFhirId(patientId);
        entity.setAppointmentDatetime(appointmentDatetime);
        entity.setPatientDisplayName(name);
        entity.setPatientPhone(phone);
        entity.setVoided(false);
        entity.setLastPolledAt(NOW);
        PolledAppointmentEntity saved = polledAppointmentRepository.save(entity);
        assertNotNull(saved.getId());
    }
}
