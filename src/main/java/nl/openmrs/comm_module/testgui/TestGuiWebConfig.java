package nl.openmrs.comm_module.testgui;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Redirect /test naar de statische testpagina. */
@Configuration
@ConditionalOnProperty(name = "comm.test-gui.enabled", havingValue = "true")
public class TestGuiWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/test", "/test-scheduling.html");
        registry.addRedirectViewController("/test/", "/test-scheduling.html");
    }
}
