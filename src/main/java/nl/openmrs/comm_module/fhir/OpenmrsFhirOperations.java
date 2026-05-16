package nl.openmrs.comm_module.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.util.Optional;

/** Contract voor FHIR-calls naar OpenMRS; los van transport/retry-implementatie. */
public interface OpenmrsFhirOperations {

    String fetchServerSoftwareNameAndVersion();

    Bundle searchEncountersSince(String isoDate);

    Optional<Patient> readPatientByLogicalId(String logicalId);
}
