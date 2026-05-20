package nl.openmrs.comm_module.organisation.dto;

import nl.openmrs.comm_module.provider.MessagingProviderType;

public class OrganisationProviderConfigRequest {

    private MessagingProviderType providerType;
    private boolean enabled;
    private int priority;
    private String credentials;

    public OrganisationProviderConfigRequest() {
    }

    public MessagingProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(MessagingProviderType providerType) {
        this.providerType = providerType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }
}