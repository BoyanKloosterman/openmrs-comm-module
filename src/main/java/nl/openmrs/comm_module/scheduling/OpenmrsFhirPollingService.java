package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.fhir.OpenmrsFhirClient;
import nl.openmrs.comm_module.messaging.fhir.EncounterFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.PatientFhirMapper;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterPollDto;
import nl.openmrs.comm_module.messaging.fhir.dto.EncounterWithPatientDto;
import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenmrsFhirPollingService {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsFhirPollingService.class);
    private final OpenmrsFhirClient openmrsFhirClient;
    private final EncounterFhirMapper encounterFhirMapper;
    private final PatientFhirMapper patientFhirMapper;
    private final int encounterPollSinceDays;

    public OpenmrsFhirPollingService(
            OpenmrsFhirClient openmrsFhirClient,
            EncounterFhirMapper encounterFhirMapper,
            PatientFhirMapper patientFhirMapper,
            @Value("${openmrs.fhir.encounter-poll-since-days:30}") int encounterPollSinceDays) {
        this.openmrsFhirClient = openmrsFhirClient;
        this.encounterFhirMapper = encounterFhirMapper;
        this.patientFhirMapper = patientFhirMapper;
        this.encounterPollSinceDays = encounterPollSinceDays;
    }

    @Scheduled(fixedDelayString = "#{${openmrs.fhir.poll-interval-minutes:1} * 60 * 1000}")
    public void pollOpenmrsFhir() {
        log.debug("FHIR poll gestart");
        try {
            String info = openmrsFhirClient.fetchServerSoftwareNameAndVersion();
            log.info("FHIR server: {}", info);

            String since = LocalDate.now(ZoneOffset.UTC)
                    .minusDays(Math.max(0, encounterPollSinceDays))
                    .toString();
            Bundle bundle = openmrsFhirClient.searchEncountersSince(since);
            List<EncounterPollDto> snapshots = mapBundle(bundle);
            List<EncounterWithPatientDto> withPatients = attachPatients(snapshots);

            long metPatient = withPatients.stream().filter(e -> e.patient() != null).count();
            log.info(
                    "Encounter-poll: {} bundle-entries, {} encounters, {} met Patient (since={})",
                    bundle.hasEntry() ? bundle.getEntry().size() : 0,
                    snapshots.size(),
                    metPatient,
                    since);
        } catch (RuntimeException e) {
            // US-003-7: uitgebreide retry; nu alleen loggen zodat scheduler blijft draaien
            log.error("FHIR poll mislukt: {}", e.getMessage(), e);
        }
    }

    /** Zet bundle entries om naar DTO's; niet-Encounter entries worden overgeslagen. */
    private List<EncounterPollDto> mapBundle(Bundle bundle) {
        List<EncounterPollDto> out = new ArrayList<>();
        if (bundle == null || !bundle.hasEntry()) {
            return out;
        }
        for (var entry : bundle.getEntry()) {
            if (entry == null || !entry.hasResource()) {
                continue;
            }
            Resource resource = entry.getResource();
            if (!(resource instanceof Encounter encounter)) {
                continue;
            }
            encounterFhirMapper.mapEncounterToEncounterPollDto(encounter).ifPresent(out::add);
        }
        return out;
    }

    /**
     * Haalt per unieke patientId één keer de Patient op (FHIR read) en koppelt aan encounter.
     * Bij ontbrekende Patient: patient=null en warn-log.
     */
    private List<EncounterWithPatientDto> attachPatients(List<EncounterPollDto> encounters) {
        Map<String, Optional<PatientPollDto>> cache = new HashMap<>();
        List<EncounterWithPatientDto> out = new ArrayList<>(encounters.size());
        for (EncounterPollDto enc : encounters) {
            String pid = enc.patientId();
            Optional<PatientPollDto> patientOpt = cache.computeIfAbsent(pid, this::loadPatientPollDto);
            PatientPollDto patient = patientOpt.orElse(null);
            if (patient == null) {
                log.warn("Patient niet geladen voor encounter {} (patientId={})", enc.encounterId(), pid);
            }
            out.add(new EncounterWithPatientDto(enc, patient));
        }
        return out;
    }

    private Optional<PatientPollDto> loadPatientPollDto(String patientLogicalId) {
        return openmrsFhirClient
                .readPatientByLogicalId(patientLogicalId)
                .flatMap(patientFhirMapper::mapPatient);
    }
}
