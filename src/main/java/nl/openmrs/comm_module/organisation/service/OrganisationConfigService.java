package nl.openmrs.comm_module.organisation.service;

import nl.openmrs.comm_module.common.encryption.PgCryptoService;
import nl.openmrs.comm_module.organisation.dto.OrganisationConfigRequest;
import nl.openmrs.comm_module.organisation.dto.OrganisationConfigResponse;
import nl.openmrs.comm_module.organisation.dto.OrganisationProviderConfigRequest;
import nl.openmrs.comm_module.organisation.dto.OrganisationProviderConfigResponse;
import nl.openmrs.comm_module.persistence.dao.OrganisationConfigRepository;
import nl.openmrs.comm_module.persistence.entity.OrganisationConfigEntity;
import nl.openmrs.comm_module.persistence.entity.OrganisationProviderConfigEntity;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class OrganisationConfigService {

    private final OrganisationConfigRepository organisationConfigRepository;
    private final PgCryptoService pgCryptoService;

    public OrganisationConfigService(
            OrganisationConfigRepository organisationConfigRepository,
            PgCryptoService pgCryptoService
    ) {
        this.organisationConfigRepository = organisationConfigRepository;
        this.pgCryptoService = pgCryptoService;
    }

    @Transactional
    public OrganisationConfigResponse saveConfig(OrganisationConfigRequest request) {
        OrganisationConfigEntity entity = organisationConfigRepository
                .findByOrganisationId(request.getOrganisationId())
                .orElseGet(() -> new OrganisationConfigEntity(
                        request.getOrganisationId(),
                        request.isActive()
                ));

        entity.setOrganisationId(request.getOrganisationId());
        entity.setActive(request.isActive());

        String timezone = request.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            timezone = "Europe/Amsterdam";
        }
        entity.setTimezone(timezone);

        entity.getProviders().clear();

        for (OrganisationProviderConfigRequest providerRequest : request.getProviders()) {
            String encryptedCredentials = pgCryptoService.encrypt(providerRequest.getCredentials());

            OrganisationProviderConfigEntity providerEntity = new OrganisationProviderConfigEntity(
                    providerRequest.getProviderType(),
                    providerRequest.isEnabled(),
                    providerRequest.getPriority(),
                    encryptedCredentials
            );

            entity.addProvider(providerEntity);
        }

        OrganisationConfigEntity savedEntity = organisationConfigRepository.save(entity);

        return toResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public OrganisationConfigResponse getConfig(String organisationId) {
        OrganisationConfigEntity entity = organisationConfigRepository
                .findByOrganisationId(organisationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organisation config not found: " + organisationId
                ));

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<OrganisationProviderConfigResponse> getEnabledProviders(String organisationId) {
        OrganisationConfigEntity entity = organisationConfigRepository
                .findByOrganisationId(organisationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organisation config not found: " + organisationId
                ));

        return entity.getProviders()
                .stream()
                .filter(OrganisationProviderConfigEntity::isEnabled)
                .sorted(Comparator.comparingInt(OrganisationProviderConfigEntity::getPriority))
                .map(provider -> new OrganisationProviderConfigResponse(
                        provider.getId(),
                        provider.getProviderType(),
                        provider.isEnabled(),
                        provider.getPriority(),
                        provider.getEncryptedCredentials() != null
                                && !provider.getEncryptedCredentials().isBlank()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getDecryptedCredentials(String organisationId, MessagingProviderType providerType) {
        OrganisationConfigEntity entity = organisationConfigRepository
                .findByOrganisationId(organisationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organisation config not found: " + organisationId
                ));

        OrganisationProviderConfigEntity provider = entity.getProviders()
                .stream()
                .filter(OrganisationProviderConfigEntity::isEnabled)
                .filter(p -> p.getProviderType() == providerType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No enabled provider config found for provider "
                                + providerType
                                + " in organisation "
                                + organisationId
                ));

        return pgCryptoService.decrypt(provider.getEncryptedCredentials());
    }

    private OrganisationConfigResponse toResponse(OrganisationConfigEntity entity) {
        List<OrganisationProviderConfigResponse> providerResponses = entity.getProviders()
                .stream()
                .sorted(Comparator.comparingInt(OrganisationProviderConfigEntity::getPriority))
                .map(provider -> new OrganisationProviderConfigResponse(
                        provider.getId(),
                        provider.getProviderType(),
                        provider.isEnabled(),
                        provider.getPriority(),
                        provider.getEncryptedCredentials() != null
                                && !provider.getEncryptedCredentials().isBlank()
                ))
                .toList();

        return new OrganisationConfigResponse(
                entity.getId(),
                entity.getOrganisationId(),
                entity.isActive(),
                entity.getTimezone(),
                providerResponses,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}