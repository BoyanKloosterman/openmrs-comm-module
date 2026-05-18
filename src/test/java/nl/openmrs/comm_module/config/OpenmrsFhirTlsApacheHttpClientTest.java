package nl.openmrs.comm_module.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Controleert dat de FHIR Apache-client TLS 1.3 gebruikt op HTTPS (US-012).
 */
class OpenmrsFhirTlsApacheHttpClientTest {

    private static final String HOWS_MY_SSL = "https://www.howsmyssl.com/a/check";
    /** Publieke FHIR-server; handshake faalt met alleen TLS 1.3-client (server TLS 1.2). */
    private static final String HAPI_TLS12_ONLY = "https://hapi.fhir.org/baseR4";

    @Test
    void httpsRequest_shouldNegotiateTls13() throws Exception {
        try (CloseableHttpClient client = OpenmrsFhirTlsApacheHttpClient.create()) {
            HttpGet get = new HttpGet(HOWS_MY_SSL);
            try (CloseableHttpResponse response = client.execute(get)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                String body = EntityUtils.toString(response.getEntity());
                assertTrue(
                        body.contains("TLS 1.3"),
                        "Verwacht TLS 1.3 handshake; howsmyssl antwoord: " + body);
            }
        }
    }

    @Test
    void hapiFactory_shouldUseConfiguredHttpClient() {
        FhirContext ctx = FhirContext.forR4();
        ApacheRestfulClientFactory factory = new ApacheRestfulClientFactory(ctx);
        CloseableHttpClient httpClient = OpenmrsFhirTlsApacheHttpClient.create();
        factory.setHttpClient(httpClient);
        assertSame(httpClient, factory.getNativeHttpClient());
    }

    @Test
    void hapiFhirClient_shouldRejectServerWithoutTls13() {
        FhirContext ctx = FhirContext.forR4();
        ApacheRestfulClientFactory factory = new ApacheRestfulClientFactory(ctx);
        factory.setHttpClient(OpenmrsFhirTlsApacheHttpClient.create());
        ctx.setRestfulClientFactory(factory);

        assertThrows(
                FhirClientConnectionException.class,
                () -> ctx.newRestfulGenericClient(HAPI_TLS12_ONLY)
                        .capabilities()
                        .ofType(org.hl7.fhir.r4.model.CapabilityStatement.class)
                        .execute());
    }
}
