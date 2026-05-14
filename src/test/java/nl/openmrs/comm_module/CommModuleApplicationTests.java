package nl.openmrs.comm_module;

import nl.openmrs.comm_module.scheduling.OpenmrsFhirPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CommModuleApplicationTests {

	// Voorkomt @Scheduled FHIR-polls tijdens contextLoads (dummy OpenMRS-URL in tests)
	@MockitoBean
	@SuppressWarnings("unused")
	private OpenmrsFhirPollingService openmrsFhirPollingService;

	@Test
	void contextLoads() {
	}

}
