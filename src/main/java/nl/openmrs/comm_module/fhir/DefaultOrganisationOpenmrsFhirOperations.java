package nl.openmrs.comm_module.fhir;

import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Standaard FHIR-bean: gebruikt {@link OpenmrsFhirProperties#getOrganisationId()} (sync, test-GUI, enkele read).
 */
@Component
@Primary
public class DefaultOrganisationOpenmrsFhirOperations implements OpenmrsFhirOperations {

    private final OpenmrsFhirOperationsFactory factory;
    private final OpenmrsFhirProperties properties;

    public DefaultOrganisationOpenmrsFhirOperations(
            OpenmrsFhirOperationsFactory factory, OpenmrsFhirProperties properties) {
        this.factory = factory;
        this.properties = properties;
    }

    private OpenmrsFhirOperations delegate() {
        return factory.forOrganisation(properties.getOrganisationId());
    }

    @Override
    public String fetchServerSoftwareNameAndVersion() {
        return delegate().fetchServerSoftwareNameAndVersion();
    }

    @Override
    public Optional<Patient> readPatientByLogicalId(String logicalId) {
        return delegate().readPatientByLogicalId(logicalId);
    }

    @Override
    public Optional<Appointment> readAppointmentByLogicalId(String logicalId) {
        return delegate().readAppointmentByLogicalId(logicalId);
    }

    @Override
    public List<Appointment> searchAppointmentsBetween(Instant from, Instant to) {
        return delegate().searchAppointmentsBetween(from, to);
    }

    @Override
    public void upsertPatient(Patient patient) {
        delegate().upsertPatient(patient);
    }

    @Override
    public void upsertAppointment(Appointment appointment) {
        delegate().upsertAppointment(appointment);
    }
}
