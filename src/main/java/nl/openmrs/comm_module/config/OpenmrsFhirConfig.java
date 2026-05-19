package nl.openmrs.comm_module.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// FHIR R5 context voor HAPI (parser, client)
@Configuration
public class OpenmrsFhirConfig {

    @Bean("fhirContextR5")
    public FhirContext fhirContextR5() {
        FhirContext ctx = FhirContext.forR5();
        ApacheRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(ctx);
        CloseableHttpClient httpClient = OpenmrsFhirTlsApacheHttpClient.create();
        clientFactory.setHttpClient(httpClient);
        ctx.setRestfulClientFactory(clientFactory);
        return ctx;
    }
}
