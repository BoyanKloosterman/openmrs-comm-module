package nl.openmrs.comm_module.provider.securepost;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SecurePostClient {

    private final RestClient restClient;
    private final SecurePostAuthClient authClient;
    private final String studentGroup;

    public SecurePostClient(
            RestClient.Builder restClientBuilder,
            SecurePostAuthClient authClient,
            @Value("${providers.base-url}") String providersBaseUrl,
            @Value("${providers.student-group}") String studentGroup
    ) {
        this.restClient = restClientBuilder
                .baseUrl(providersBaseUrl)
                .build();

        this.authClient = authClient;
        this.studentGroup = studentGroup;
    }

    public SecurePostResponse send(
            SecurePostRequest request,
            String clientId,
            String clientSecret
    ) {
        String token = authClient.getToken(clientId, clientSecret);

        try {
            return restClient.post()
                    .uri("/securepost/message")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-STUDENT-GROUP", studentGroup)
                    .header("Authorization", "Bearer " + token)
                    .body(request)
                    .retrieve()
                    .body(SecurePostResponse.class);
        } catch (RestClientResponseException exception) {
            throw new SecurePostApiException(
                    "SecurePost API error. Status: " + exception.getStatusCode()
                            + ". Body: " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new SecurePostApiException(
                    "SecurePost request failed: " + exception.getMessage(),
                    exception
            );
        }
    }
}