package nl.openmrs.comm_module.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** MariaDB van reference distro; alleen JdbcTemplate (geen tweede DataSource-bean voor JPA). */
@Configuration
@ConditionalOnProperty(prefix = "openmrs.datasource", name = "url")
public class OpenmrsDataSourceConfig {

    @Bean(name = "openmrsJdbcTemplate")
    public JdbcTemplate openmrsJdbcTemplate(OpenmrsDataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(properties.getUrl().trim());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setMaximumPoolSize(3);
        ds.setPoolName("openmrs-distro");
        return new JdbcTemplate(ds);
    }
}
