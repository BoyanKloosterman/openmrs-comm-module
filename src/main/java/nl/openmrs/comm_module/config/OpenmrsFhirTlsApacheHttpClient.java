package nl.openmrs.comm_module.config;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;

/**
 * Apache HttpClient voor HAPI FHIR met alleen TLS 1.3 op HTTPS (US-012).
 */
final class OpenmrsFhirTlsApacheHttpClient {

    private OpenmrsFhirTlsApacheHttpClient() {
    }

    static CloseableHttpClient create() {
        SSLContext sslContext = SSLContexts.createDefault();
        SSLConnectionSocketFactory httpsFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.3"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", httpsFactory)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(20);
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .useSystemProperties()
                .disableCookieManagement()
                .build();
    }
}
