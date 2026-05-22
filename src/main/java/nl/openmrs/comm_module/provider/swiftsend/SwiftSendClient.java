package nl.openmrs.comm_module.provider.swiftsend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SwiftSendClient {

    private final WebClient webClient;
    private final String studentGroup;

    public SwiftSendClient(
            WebClient.Builder webClientBuilder,
            @Value("${providers.base-url}") String baseUrl,
            @Value("${providers.student-group}") String studentGroup
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.studentGroup = studentGroup;
    }

    public SwiftSendResponse send(SwiftSendRequest request, String apiKey) {
        return webClient.post()
                .uri("/swiftsend")
                .header("X-API-KEY", apiKey)
                .header("X-STUDENT-GROUP", studentGroup)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .map(body -> new SwiftSendApiException(
                                        "SwiftSend API error. Status: "
                                                + response.statusCode()
                                                + ". Body: "
                                                + body
                                ))
                )
                .bodyToMono(SwiftSendResponse.class)
                .block();
    }
}