package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.fhir.OpenmrsFhirClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OpenmrsFhirPollingService {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsFhirPollingService.class);
    private final OpenmrsFhirClient openmrsFhirClient;

    public OpenmrsFhirPollingService(OpenmrsFhirClient openmrsFhirClient) {
        this.openmrsFhirClient = openmrsFhirClient;
    }

    @Scheduled(fixedDelayString = "#{${openmrs.fhir.poll-interval-minutes:1} * 60 * 1000}")
    public void pollOpenmrsFhir() {
        log.debug("FHIR poll gestart");
        String info = openmrsFhirClient.fetchServerSoftwareNameAndVersion();
        log.info("FHIR server: {}", info);
    }
}