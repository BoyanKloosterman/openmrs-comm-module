package nl.openmrs.comm_module.organisation.service;

import nl.openmrs.comm_module.common.encryption.PgCryptoService;
import nl.openmrs.comm_module.organisation.dto.OrganisationConfigRequest;
import nl.openmrs.comm_module.organisation.dto.OrganisationConfigResponse;
import nl.openmrs.comm_module.organisation.dto.OrganisationProviderConfigRequest;
import nl.openmrs.comm_module.organisation.dto.OrganisationProviderConfigResponse;
import nl.openmrs.comm_module.persistence.dao.OrganisationConfigRepository;
import nl.openmrs.comm_module.persistence.entity.OrganisationConfigEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganisationConfigServiceTest {

  private OrganisationConfigRepository organisationConfigRepository;
  private PgCryptoService pgCryptoService;
  private OrganisationConfigService organisationConfigService;

  @BeforeEach
  void setUp() {
    organisationConfigRepository = mock(OrganisationConfigRepository.class);
    pgCryptoService = mock(PgCryptoService.class);

    organisationConfigService = new OrganisationConfigService(
        organisationConfigRepository,
        pgCryptoService);
  }

  @Test
  void saveConfigCreatesOrganisationWithMultipleProviders() {
    OrganisationConfigRequest request = createRequest();

    when(organisationConfigRepository.findByOrganisationId("openmrs-demo"))
        .thenReturn(Optional.empty());

    when(pgCryptoService.encrypt("{\"apiKey\":\"test-swiftsend-key\"}"))
        .thenReturn("encrypted-swiftsend");

    when(pgCryptoService.encrypt("{\"clientId\":\"securepost-client\",\"clientSecret\":\"securepost-secret\"}"))
        .thenReturn("encrypted-securepost");

    when(organisationConfigRepository.save(any(OrganisationConfigEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrganisationConfigResponse response = organisationConfigService.saveConfig(request);

    assertEquals("openmrs-demo", response.getOrganisationId());
    assertTrue(response.isActive());
    assertEquals("Europe/Amsterdam", response.getTimezone());
    assertEquals(2, response.getProviders().size());

    OrganisationProviderConfigResponse firstProvider = response.getProviders().get(0);
    OrganisationProviderConfigResponse secondProvider = response.getProviders().get(1);

    assertEquals(MessagingProviderType.SWIFTSEND, firstProvider.getProviderType());
    assertEquals(1, firstProvider.getPriority());
    assertTrue(firstProvider.isCredentialsConfigured());

    assertEquals(MessagingProviderType.SECUREPOST, secondProvider.getProviderType());
    assertEquals(2, secondProvider.getPriority());
    assertTrue(secondProvider.isCredentialsConfigured());

    verify(pgCryptoService).encrypt("{\"apiKey\":\"test-swiftsend-key\"}");
    verify(pgCryptoService).encrypt("{\"clientId\":\"securepost-client\",\"clientSecret\":\"securepost-secret\"}");
    verify(organisationConfigRepository).save(any(OrganisationConfigEntity.class));
  }

  @Test
  void getEnabledProvidersReturnsOnlyEnabledProvidersSortedByPriority() {
    OrganisationConfigEntity entity = new OrganisationConfigEntity("openmrs-demo", true);

    entity.addProvider(new nl.openmrs.comm_module.persistence.entity.OrganisationProviderConfigEntity(
        MessagingProviderType.SECUREPOST,
        true,
        2,
        "encrypted-securepost"));

    entity.addProvider(new nl.openmrs.comm_module.persistence.entity.OrganisationProviderConfigEntity(
        MessagingProviderType.SWIFTSEND,
        true,
        1,
        "encrypted-swiftsend"));

    entity.addProvider(new nl.openmrs.comm_module.persistence.entity.OrganisationProviderConfigEntity(
        MessagingProviderType.LEGACYLINK,
        false,
        3,
        "encrypted-legacylink"));

    when(organisationConfigRepository.findByOrganisationId("openmrs-demo"))
        .thenReturn(Optional.of(entity));

    List<OrganisationProviderConfigResponse> providers = organisationConfigService.getEnabledProviders("openmrs-demo");

    assertEquals(2, providers.size());
    assertEquals(MessagingProviderType.SWIFTSEND, providers.get(0).getProviderType());
    assertEquals(MessagingProviderType.SECUREPOST, providers.get(1).getProviderType());
  }

  @Test
  void saveConfigWithCustomTimezone_shouldPreserveTimezone() {
    OrganisationConfigRequest request = createRequest();
    request.setTimezone("Asia/Tokyo");

    when(organisationConfigRepository.findByOrganisationId("openmrs-demo"))
        .thenReturn(Optional.empty());

    when(pgCryptoService.encrypt(any()))
        .thenReturn("encrypted");

    when(organisationConfigRepository.save(any(OrganisationConfigEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrganisationConfigResponse response = organisationConfigService.saveConfig(request);

    assertEquals("Asia/Tokyo", response.getTimezone());
  }

  @Test
  void saveConfigWithNullTimezone_shouldUseDefaultTimezone() {
    OrganisationConfigRequest request = createRequest();
    request.setTimezone(null);

    when(organisationConfigRepository.findByOrganisationId("openmrs-demo"))
        .thenReturn(Optional.empty());

    when(pgCryptoService.encrypt(any()))
        .thenReturn("encrypted");

    when(organisationConfigRepository.save(any(OrganisationConfigEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrganisationConfigResponse response = organisationConfigService.saveConfig(request);

    assertEquals("Europe/Amsterdam", response.getTimezone());
  }

  @Test
  void saveConfigWithBlankTimezone_shouldUseDefaultTimezone() {
    OrganisationConfigRequest request = createRequest();
    request.setTimezone("   ");

    when(organisationConfigRepository.findByOrganisationId("openmrs-demo"))
        .thenReturn(Optional.empty());

    when(pgCryptoService.encrypt(any()))
        .thenReturn("encrypted");

    when(organisationConfigRepository.save(any(OrganisationConfigEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrganisationConfigResponse response = organisationConfigService.saveConfig(request);

    assertEquals("Europe/Amsterdam", response.getTimezone());
  }

  private OrganisationConfigRequest createRequest() {
    OrganisationProviderConfigRequest swiftSend = new OrganisationProviderConfigRequest();
    swiftSend.setProviderType(MessagingProviderType.SWIFTSEND);
    swiftSend.setEnabled(true);
    swiftSend.setPriority(1);
    swiftSend.setCredentials("{\"apiKey\":\"test-swiftsend-key\"}");

    OrganisationProviderConfigRequest securePost = new OrganisationProviderConfigRequest();
    securePost.setProviderType(MessagingProviderType.SECUREPOST);
    securePost.setEnabled(true);
    securePost.setPriority(2);
    securePost.setCredentials("{\"clientId\":\"securepost-client\",\"clientSecret\":\"securepost-secret\"}");

    OrganisationConfigRequest request = new OrganisationConfigRequest();
    request.setOrganisationId("openmrs-demo");
    request.setActive(true);
    request.setProviders(List.of(swiftSend, securePost));

    return request;
  }
}