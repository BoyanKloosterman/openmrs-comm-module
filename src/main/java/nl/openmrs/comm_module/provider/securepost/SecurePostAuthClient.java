package nl.openmrs.comm_module.provider.securepost;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SecurePostAuthClient {

    private final RestClient restClient;
    private final String studentGroup;
    private final String clientId;
    private final String clientSecret;

    public SecurePostAuthClient(
            RestClient.Builder restClientBuilder,
            @Value("${providers.base-url}") String providersBaseUrl,
            @Value("${providers.student-group}") String studentGroup,
            @Value("${providers.securepost.client-id}") String clientId,
            @Value("${providers.securepost.client-secret}") String clientSecret
    ) {
        this.restClient = restClientBuilder
                .baseUrl(providersBaseUrl)
                .build();

        this.studentGroup = studentGroup;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getToken() {
        try {
            SecurePostTokenResponse response = restClient.post()
                    .uri("/securepost/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-STUDENT-GROUP", studentGroup)
                    .body(new SecurePostAuthRequest(clientId, clientSecret))
                    .retrieve()
                    .body(SecurePostTokenResponse.class);

            if (response == null || response.getAccessToken() == null || response.getAccessToken().isBlank()) {
                throw new SecurePostApiException("SecurePost auth returned no access token");
            }

            return response.getAccessToken();
        } catch (RestClientResponseException exception) {
            throw new SecurePostApiException(
                    "SecurePost auth error. Status: " + exception.getStatusCode()
                            + ". Body: " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new SecurePostApiException(
                    "SecurePost auth request failed: " + exception.getMessage(),
                    exception
            );
        }
    }
}