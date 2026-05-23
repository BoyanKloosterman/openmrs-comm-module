package nl.openmrs.comm_module.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import nl.openmrs.comm_module.messaging.fhir.FhirMessageParseException;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Spring HttpMessageConverter for FHIR R5 resources.
 * 
 * <p>
 * Handles automatic serialization/deserialization of FHIR R5 resources
 * in HTTP request/response bodies (US-009 enhancement).
 * 
 * <p>
 * Supported media types:
 * - application/fhir+json
 * - application/json
 */
@Component
public class FhirR5HttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

  private final FhirContext fhirContext;

  public FhirR5HttpMessageConverter(FhirContext fhirContext) {
    super(MediaType.APPLICATION_JSON, new MediaType("application", "fhir+json"));
    this.fhirContext = fhirContext;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Resource.class.isAssignableFrom(clazz);
  }

  @Override
  protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
      throws IOException {
    try (InputStreamReader reader = new InputStreamReader(
        inputMessage.getBody(), StandardCharsets.UTF_8)) {
      Resource resource = (Resource) fhirContext.newJsonParser().parseResource(reader);
      if (resource == null) {
        throw new FhirMessageParseException("Lege of ongeldige FHIR request body");
      }
      if (Bundle.class.isAssignableFrom(clazz) && !(resource instanceof Bundle)) {
        throw new FhirMessageParseException(
            "Verwacht resourceType Bundle, maar ontving: " + resource.fhirType());
      }
      return resource;
    } catch (FhirMessageParseException ex) {
      throw ex;
    } catch (DataFormatException ex) {
      throw new FhirMessageParseException("Ongeldige FHIR JSON: " + ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new FhirMessageParseException(
          "Kon FHIR-bericht niet parsen: " + ex.getMessage(), ex);
    }
  }

  @Override
  protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
      throws IOException {
    outputMessage.getHeaders().setContentType(new MediaType("application", "fhir+json"));
    try (OutputStreamWriter writer = new OutputStreamWriter(
        outputMessage.getBody(), StandardCharsets.UTF_8)) {
      String json = fhirContext.newJsonParser()
          .setPrettyPrint(true)
          .encodeResourceToString(resource);
      writer.write(json);
      writer.flush();
    }
  }
}
