package nl.openmrs.comm_module.poll;

import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentWithPatientDto;

import java.util.List;

public interface AppointmentPollPersistence {

    void upsertPollResults(String organisationId, List<AppointmentWithPatientDto> appointmentsWithPatients);
}
