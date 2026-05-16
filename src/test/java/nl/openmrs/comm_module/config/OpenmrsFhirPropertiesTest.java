package nl.openmrs.comm_module.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenmrsFhirPropertiesTest {

    @Test
    void pollDelayGebruiktOrganisatieOverride() {
        OpenmrsFhirProperties p = new OpenmrsFhirProperties();
        p.setOrganisationId("acme");
        p.setPollIntervalMinutes(10);
        OpenmrsFhirProperties.OrganisationPollSettings o = new OpenmrsFhirProperties.OrganisationPollSettings();
        o.setPollIntervalMinutes(3);
        p.getOrganisations().put("acme", o);
        assertEquals(3 * 60_000L, p.pollDelayMillis());
    }

    @Test
    void pollDelayValtTerugOpGlobaalInterval() {
        OpenmrsFhirProperties p = new OpenmrsFhirProperties();
        p.setOrganisationId("acme");
        p.setPollIntervalMinutes(7);
        assertEquals(7 * 60_000L, p.pollDelayMillis());
    }
}
