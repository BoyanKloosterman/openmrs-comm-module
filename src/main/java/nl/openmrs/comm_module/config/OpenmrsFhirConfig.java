package nl.openmrs.comm_module.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// FHIR R4 context voor HAPI (parser, client-helper)
@Configuration
public class OpenmrsFhirConfig {

    @Bean("fhirContextR4")
    public FhirContext fhirContextR4() {
        return FhirContext.forR4();
    }
}