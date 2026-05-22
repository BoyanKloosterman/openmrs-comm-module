package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.OpenmrsDataSourceProperties;
import nl.openmrs.comm_module.config.OpenmrsFhirProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenmrsPollOrganisationScopeTest {

    @Test
    void jdbcZonderFhirUrlGeeftDefaultOrganisatie() {
        OpenmrsFhirProperties fhir = new OpenmrsFhirProperties();
        fhir.setOrganisationId("default");
        fhir.setServerUrl("");

        OpenmrsDataSourceProperties ds = new OpenmrsDataSourceProperties();
        ds.setUrl("jdbc:mariadb://localhost:3307/openmrs");

        OpenmrsPollOrganisationScope scope = new OpenmrsPollOrganisationScope(fhir, ds, "jdbc", true);

        assertEquals(1, scope.activeOrganisationIds().size());
        assertEquals("default", scope.activeOrganisationIds().get(0));
        assertEquals(1, scope.activePollConnections().size());
    }

    @Test
    void zonderFhirEnZonderJdbcLeeg() {
        OpenmrsFhirProperties fhir = new OpenmrsFhirProperties();
        fhir.setServerUrl("");

        OpenmrsPollOrganisationScope scope =
                new OpenmrsPollOrganisationScope(fhir, new OpenmrsDataSourceProperties(), "jdbc", true);

        assertTrue(scope.activeOrganisationIds().isEmpty());
    }

    @Test
    void fhirMetUrlGebruiktFhirConnection() {
        OpenmrsFhirProperties fhir = new OpenmrsFhirProperties();
        fhir.setOrganisationId("default");
        fhir.setServerUrl("http://localhost/openmrs/ws/fhir2/R5");

        OpenmrsDataSourceProperties ds = new OpenmrsDataSourceProperties();
        ds.setUrl("jdbc:mariadb://localhost:3307/openmrs");

        OpenmrsPollOrganisationScope scope = new OpenmrsPollOrganisationScope(fhir, ds, "fhir", true);

        assertEquals(1, scope.activePollConnections().size());
        assertEquals("http://localhost/openmrs/ws/fhir2/R5", scope.activePollConnections().get(0).serverUrl());
    }

    @Test
    void fhirZonderUrlMetJdbcFallback() {
        OpenmrsFhirProperties fhir = new OpenmrsFhirProperties();
        fhir.setOrganisationId("default");
        fhir.setServerUrl("");

        OpenmrsDataSourceProperties ds = new OpenmrsDataSourceProperties();
        ds.setUrl("jdbc:mariadb://localhost:3307/openmrs");

        OpenmrsPollOrganisationScope scope = new OpenmrsPollOrganisationScope(fhir, ds, "fhir", true);

        assertEquals(1, scope.activeOrganisationIds().size());
        assertEquals("jdbc:openmrs", scope.activePollConnections().get(0).serverUrl());
    }
}
