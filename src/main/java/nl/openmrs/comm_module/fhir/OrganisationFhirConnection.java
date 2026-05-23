package nl.openmrs.comm_module.fhir;

/** FHIR R5-bron voor één OpenMRS-organisatie (US-003 multi-tenant). */
public record OrganisationFhirConnection(
        String organisationId,
        String serverUrl,
        String username,
        String password,
        int pollIntervalMinutes,
        int appointmentPollSinceDays) {

    public boolean hasAuth() {
        return username != null && !username.isBlank();
    }
}
