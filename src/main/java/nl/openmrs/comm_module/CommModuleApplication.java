package nl.openmrs.comm_module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CommModuleApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommModuleApplication.class, args);
	}

}
