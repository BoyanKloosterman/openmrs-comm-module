package nl.openmrs.comm_module.provider.legacylink;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class LegacyLinkClient {

    private final RestClient restClient;
    private final String username;
    private final String password;
    private final String studentGroup;

    public LegacyLinkClient(
            RestClient.Builder restClientBuilder,
            @Value("${providers.base-url}") String providersBaseUrl,
            @Value("${providers.legacylink.username}") String username,
            @Value("${providers.legacylink.password}") String password,
            @Value("${providers.student-group}") String studentGroup
    ) {
        this.restClient = restClientBuilder
                .baseUrl(providersBaseUrl)
                .build();

        this.username = username;
        this.password = password;
        this.studentGroup = studentGroup;
    }

    public LegacyLinkSoapResponse sendSms(LegacyLinkSoapRequest request) {
        try {
            String responseXml = restClient.post()
                    .uri("/LegacyLink/SendSms")
                    .contentType(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_XML)
                    .headers(headers -> headers.setBasicAuth(username, password))
                    .header("X-STUDENT-GROUP", studentGroup)
                    .body(request.toXml())
                    .retrieve()
                    .body(String.class);

            if (responseXml == null || responseXml.isBlank()) {
                throw new LegacyLinkApiException("LegacyLink returned an empty response");
            }

            return LegacyLinkSoapResponse.fromXml(responseXml);
        } catch (RestClientResponseException exception) {
            String responseBody = exception.getResponseBodyAsString();

            if (responseBody != null && !responseBody.isBlank()) {
                return LegacyLinkSoapResponse.fromXml(responseBody);
            }

            throw new LegacyLinkApiException(
                    "LegacyLink API error. Status: " + exception.getStatusCode(),
                    exception
            );
        } catch (LegacyLinkApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LegacyLinkApiException(
                    "LegacyLink request failed: " + exception.getMessage(),
                    exception
            );
        }
    }
}