package nl.openmrs.comm_module.provider.asyncflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AsyncFlowClient {

    private final RestClient restClient;
    private final String studentGroup;

    public AsyncFlowClient(
            RestClient.Builder restClientBuilder,
            @Value("${providers.base-url}") String providersBaseUrl,
            @Value("${providers.student-group}") String studentGroup
    ) {
        this.restClient = restClientBuilder
                .baseUrl(providersBaseUrl)
                .build();

        this.studentGroup = studentGroup;
    }

    public AsyncFlowSubmitResponse submit(
            AsyncFlowSubmitRequest request,
            String apiKey
    ) {
        try {
            return restClient.post()
                    .uri("/asyncflow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-KEY", apiKey)
                    .header("X-STUDENT-GROUP", studentGroup)
                    .body(request)
                    .retrieve()
                    .body(AsyncFlowSubmitResponse.class);
        } catch (RestClientResponseException exception) {
            throw new AsyncFlowApiException(
                    "AsyncFlow API error. Status: " + exception.getStatusCode()
                            + ". Body: " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new AsyncFlowApiException(
                    "AsyncFlow request failed: " + exception.getMessage(),
                    exception
            );
        }
    }

    public AsyncFlowStatusResponse getStatus(
            String trackingId,
            String apiKey
    ) {
        try {
            return restClient.get()
                    .uri("/asyncflow/{trackingId}", trackingId)
                    .header("X-API-KEY", apiKey)
                    .header("X-STUDENT-GROUP", studentGroup)
                    .retrieve()
                    .body(AsyncFlowStatusResponse.class);
        } catch (RestClientResponseException exception) {
            throw new AsyncFlowApiException(
                    "AsyncFlow status API error. Status: " + exception.getStatusCode()
                            + ". Body: " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new AsyncFlowApiException(
                    "AsyncFlow status request failed: " + exception.getMessage(),
                    exception
            );
        }
    }
}