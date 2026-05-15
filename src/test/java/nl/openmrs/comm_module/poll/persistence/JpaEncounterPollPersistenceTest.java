package nl.openmrs.comm_module.poll.persistence;

import nl.openmrs.comm_module.messaging.fhir.dto.EncounterPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import nl.openmrs.comm_module.poll.EncounterPollPersistence;
import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class JpaEncounterPollPersistenceTest {

    @MockitoBean
    @SuppressWarnings("unused")
    private OpenmrsFhirPollingService openmrsFhirPollingService;

    @Autowired
    private PolledEncounterRepository repository;

    @Autowired
    private EncounterPollPersistence persistence;

    @Test
    void upsertWerktEnWerktIdempotent() {
        Instant t = Instant.parse("2026-05-01T12:00:00Z");
        EncounterPollDto enc = new EncounterPollDto("uuid-1", "enc-1", "pat-1", t, "loc-1", "type", false);
        PatientPollDto patient = new PatientPollDto("pat-1", "Jan", "+31000");
        List<EncounterWithPatientDto> batch = List.of(new EncounterWithPatientDto(enc, patient));

        persistence.upsertPollResults("org-a", batch);
        persistence.upsertPollResults("org-a", batch);

        List<PolledEncounterEntity> all = repository.findAll();
        assertEquals(1, all.size());
        PolledEncounterEntity e = all.get(0);
        assertEquals("org-a", e.getOrganisationId());
        assertEquals("Jan", e.getPatientDisplayName());
        assertEquals("type", e.getEncounterType());
    }

    @Test
    void updateOverschrijftVelden() {
        Instant t1 = Instant.parse("2026-05-01T12:00:00Z");
        EncounterPollDto first = new EncounterPollDto("uuid-1", "enc-9", "pat-9", t1, null, "oud", false);
        persistence.upsertPollResults("org-b", List.of(new EncounterWithPatientDto(first, null)));

        Instant t2 = Instant.parse("2026-06-01T08:00:00Z");
        EncounterPollDto second =
                new EncounterPollDto("uuid-1", "enc-9", "pat-9", t2, "loc-x", "nieuw", true);
        PatientPollDto patient = new PatientPollDto("pat-9", "Piet", "+31999");
        persistence.upsertPollResults("org-b", List.of(new EncounterWithPatientDto(second, patient)));

        Optional<PolledEncounterEntity> stored =
                repository.findByOrganisationIdAndEncounterFhirId("org-b", "enc-9");
        assertTrue(stored.isPresent());
        assertEquals("nieuw", stored.get().getEncounterType());
        assertTrue(stored.get().isVoided());
        assertEquals("+31999", stored.get().getPatientPhone());
        assertEquals(t2, stored.get().getEncounterDatetime());
    }
}
