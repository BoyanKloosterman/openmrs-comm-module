package nl.openmrs.comm_module.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// FHIR R4 context voor HAPI (parser, client-helper)
@Configuration
public class OpenmrsFhirConfig {

    @Bean("fhirContextR4")
    public FhirContext fhirContextR4() {
        FhirContext ctx = FhirContext.forR4();
        ApacheRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(ctx);
        CloseableHttpClient httpClient = OpenmrsFhirTlsApacheHttpClient.create();
        clientFactory.setHttpClient(httpClient);
        ctx.setRestfulClientFactory(clientFactory);
        return ctx;
    }
}

