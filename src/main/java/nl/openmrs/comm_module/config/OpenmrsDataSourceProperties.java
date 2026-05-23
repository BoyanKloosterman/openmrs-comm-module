package nl.openmrs.comm_module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Optionele JDBC-koppeling naar OpenMRS MariaDB (reference distro SPA). */
@Component
@ConfigurationProperties(prefix = "openmrs.datasource")
public class OpenmrsDataSourceProperties {

    private String url = "";
    private String username = "";
    private String password = "";

    public boolean isConfigured() {
        return url != null && !url.isBlank();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
