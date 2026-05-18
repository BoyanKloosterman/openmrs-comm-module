package nl.openmrs.comm_module.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.util.Arrays;

@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder(Environment env) {
        try {
            SslContext sslContext = SslContextBuilder.forClient()
                    .protocols("TLSv1.3")
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(ssl -> ssl.sslContext(sslContext));

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient));
        } catch (SSLException e) {
            if (hasDevProfile(env)) {
                log.warn("WebClient: TLSv1.3 SSLContext mislukt; fallback zonder expliciete TLS (alleen dev-profiel)", e);
                return WebClient.builder();
            }
            throw new IllegalStateException(
                    "WebClient vereist TLSv1.3 SSLContext; voor lokale fallback: spring.profiles.active=dev", e);
        }
    }

    private static boolean hasDevProfile(Environment env) {
        return Arrays.stream(env.getActiveProfiles()).anyMatch("dev"::equals);
    }
}
