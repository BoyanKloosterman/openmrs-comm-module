package nl.openmrs.comm_module.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registreert de FHIR R5 message converter voor HTTP request/response (US-009).
 */
@Configuration
public class FhirWebMvcConfig implements WebMvcConfigurer {

  private final FhirR5HttpMessageConverter fhirR5HttpMessageConverter;

  public FhirWebMvcConfig(FhirR5HttpMessageConverter fhirR5HttpMessageConverter) {
    this.fhirR5HttpMessageConverter = fhirR5HttpMessageConverter;
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(0, fhirR5HttpMessageConverter);
  }
}
