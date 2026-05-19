package nl.openmrs.comm_module.fhir;

import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Patient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Contract voor FHIR R5-calls naar OpenMRS; los van transport/retry-implementatie. */
public interface OpenmrsFhirOperations {

    String fetchServerSoftwareNameAndVersion();

    Optional<Patient> readPatientByLogicalId(String logicalId);

    List<Appointment> searchAppointmentsBetween(Instant from, Instant to);
}
