package nl.openmrs.comm_module.poll.source;

import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;

import java.time.Instant;
import java.util.List;

/** Haalt afspraken + patiënt op (OCP: andere bron = nieuwe implementatie). */
public interface AppointmentPollSource {

    List<AppointmentWithPatientDto> fetchBetween(Instant from, Instant to);
}
