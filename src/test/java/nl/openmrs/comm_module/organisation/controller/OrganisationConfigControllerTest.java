package nl.openmrs.comm_module.organisation.controller;

import nl.openmrs.comm_module.organisation.dto.OrganisationConfigResponse;
import nl.openmrs.comm_module.organisation.dto.OrganisationProviderConfigResponse;
import nl.openmrs.comm_module.organisation.service.OrganisationConfigService;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrganisationConfigControllerTest {

    private OrganisationConfigService organisationConfigService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        organisationConfigService = mock(OrganisationConfigService.class);

        OrganisationConfigController controller =
                new OrganisationConfigController(organisationConfigService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void saveConfigReturnsCreatedOrganisationConfig() throws Exception {
        when(organisationConfigService.saveConfig(any()))
                .thenReturn(createResponse());

        String requestBody = """
                {
                  "organisationId": "openmrs-demo",
                  "active": true,
                  "timezone": "Europe/Amsterdam",
                  "providers": [
                    {
                      "providerType": "SWIFTSEND",
                      "enabled": true,
                      "priority": 1,
                      "credentials": "{\\"apiKey\\":\\"test-swiftsend-key\\"}"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/organisations/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organisationId").value("openmrs-demo"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.timezone").value("Europe/Amsterdam"))
                .andExpect(jsonPath("$.providers[0].providerType").value("SWIFTSEND"))
                .andExpect(jsonPath("$.providers[0].enabled").value(true))
                .andExpect(jsonPath("$.providers[0].priority").value(1))
                .andExpect(jsonPath("$.providers[0].credentialsConfigured").value(true));
    }

    @Test
    void getConfigReturnsOrganisationConfig() throws Exception {
        when(organisationConfigService.getConfig("openmrs-demo"))
                .thenReturn(createResponse());

        mockMvc.perform(get("/api/organisations/config/openmrs-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organisationId").value("openmrs-demo"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.timezone").value("Europe/Amsterdam"))
                .andExpect(jsonPath("$.providers[0].providerType").value("SWIFTSEND"));
    }

    @Test
    void getEnabledProvidersReturnsProviders() throws Exception {
        when(organisationConfigService.getEnabledProviders("openmrs-demo"))
                .thenReturn(List.of(
                        new OrganisationProviderConfigResponse(
                                UUID.randomUUID(),
                                MessagingProviderType.SWIFTSEND,
                                true,
                                1,
                                true
                        )
                ));

        mockMvc.perform(get("/api/organisations/config/openmrs-demo/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerType").value("SWIFTSEND"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].priority").value(1))
                .andExpect(jsonPath("$[0].credentialsConfigured").value(true));
    }

    private OrganisationConfigResponse createResponse() {
        return new OrganisationConfigResponse(
                UUID.randomUUID(),
                "openmrs-demo",
                true,
                "Europe/Amsterdam",
                List.of(
                        new OrganisationProviderConfigResponse(
                                UUID.randomUUID(),
                                MessagingProviderType.SWIFTSEND,
                                true,
                                1,
                                true
                        )
                ),
                Instant.now(),
                Instant.now()
        );
    }
}