package nl.openmrs.comm_module.organisation.dto;

import nl.openmrs.comm_module.provider.MessagingProviderType;

import java.util.UUID;

public class OrganisationProviderConfigResponse {

    private UUID id;
    private MessagingProviderType providerType;
    private boolean enabled;
    private int priority;
    private boolean credentialsConfigured;

    public OrganisationProviderConfigResponse(
            UUID id,
            MessagingProviderType providerType,
            boolean enabled,
            int priority,
            boolean credentialsConfigured
    ) {
        this.id = id;
        this.providerType = providerType;
        this.enabled = enabled;
        this.priority = priority;
        this.credentialsConfigured = credentialsConfigured;
    }

    public UUID getId() {
        return id;
    }

    public MessagingProviderType getProviderType() {
        return providerType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isCredentialsConfigured() {
        return credentialsConfigured;
    }
}