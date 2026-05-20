package nl.openmrs.comm_module.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SchedulingClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
