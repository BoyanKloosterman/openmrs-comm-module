package nl.openmrs.comm_module.config;

import nl.openmrs.comm_module.fhir.OrganisationFhirConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenmrsFhirPropertiesTest {

    @Test
    void pollDelayGebruiktKortsteOrganisatieInterval() {
        OpenmrsFhirProperties p = new OpenmrsFhirProperties();
        p.setServerUrl("http://default/fhir");
        p.setOrganisationId("default");
        p.setPollIntervalMinutes(10);
        OpenmrsFhirProperties.OrganisationFhirSettings acme = new OpenmrsFhirProperties.OrganisationFhirSettings();
        acme.setServerUrl("http://acme/fhir");
        acme.setPollIntervalMinutes(3);
        p.getOrganisations().put("acme", acme);
        assertEquals(3 * 60_000L, p.pollDelayMillis());
    }

    @Test
    void pollDelayValtTerugOpGlobaalInterval() {
        OpenmrsFhirProperties p = new OpenmrsFhirProperties();
        p.setServerUrl("http://default/fhir");
        p.setOrganisationId("acme");
        p.setPollIntervalMinutes(7);
        assertEquals(7 * 60_000L, p.pollDelayMillis());
    }

    @Test
    void resolveActiveConnectionsMeerdereOrganisaties() {
        OpenmrsFhirProperties p = new OpenmrsFhirProperties();
        p.setServerUrl("http://should-not-use/fhir");
        p.setOrganisationId("default");

        OpenmrsFhirProperties.OrganisationFhirSettings a = settings("http://a/fhir", 5);
        OpenmrsFhirProperties.OrganisationFhirSettings b = settings("http://b/fhir", 10);
        p.getOrganisations().put("org-a", a);
        p.getOrganisations().put("org-b", b);

        assertEquals(2, p.resolveActiveConnections().size());
        assertTrue(p.resolveActiveConnections().stream()
                .map(OrganisationFhirConnection::organisationId)
                .toList()
                .containsAll(java.util.List.of("org-a", "org-b")));
    }

    @Test
    void resolveActiveConnectionsFallbackNaarGlobaleUrl() {
        OpenmrsFhirProperties p = new OpenmrsFhirProperties();
        p.setServerUrl("http://single/fhir");
        p.setOrganisationId("demo");
        p.setPollIntervalMinutes(2);
        p.setAppointmentPollSinceDays(14);

        assertEquals(1, p.resolveActiveConnections().size());
        OrganisationFhirConnection c = p.resolveActiveConnections().get(0);
        assertEquals("demo", c.organisationId());
        assertEquals("http://single/fhir", c.serverUrl());
        assertEquals(2, c.pollIntervalMinutes());
        assertEquals(14, c.appointmentPollSinceDays());
    }

    @Test
    void pollDelayMetOrganisationPollSettingsAlias() {
        OpenmrsFhirProperties p = new OpenmrsFhirProperties();
        p.setServerUrl("http://default/fhir");
        p.setOrganisationId("acme");
        p.setPollIntervalMinutes(10);
        OpenmrsFhirProperties.OrganisationPollSettings o = new OpenmrsFhirProperties.OrganisationPollSettings();
        o.setServerUrl("http://acme/fhir");
        o.setPollIntervalMinutes(3);
        p.getOrganisations().put("acme", o);
        assertEquals(3 * 60_000L, p.pollDelayMillis());
    }

    private static OpenmrsFhirProperties.OrganisationFhirSettings settings(String url, int interval) {
        OpenmrsFhirProperties.OrganisationFhirSettings s = new OpenmrsFhirProperties.OrganisationFhirSettings();
        s.setServerUrl(url);
        s.setPollIntervalMinutes(interval);
        return s;
    }
}
