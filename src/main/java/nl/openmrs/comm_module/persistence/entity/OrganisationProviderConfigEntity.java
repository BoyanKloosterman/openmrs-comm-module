package nl.openmrs.comm_module.persistence.entity;

import jakarta.persistence.*;
import nl.openmrs.comm_module.provider.MessagingProviderType;

import java.util.UUID;

@Entity
@Table(name = "organisation_provider_config")
public class OrganisationProviderConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_config_id", nullable = false)
    private OrganisationConfigEntity organisationConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private MessagingProviderType providerType;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int priority = 1;

    @Column(name = "encrypted_credentials", columnDefinition = "TEXT")
    private String encryptedCredentials;

    public OrganisationProviderConfigEntity() {
    }

    public OrganisationProviderConfigEntity(
            MessagingProviderType providerType,
            boolean enabled,
            int priority,
            String encryptedCredentials
    ) {
        this.providerType = providerType;
        this.enabled = enabled;
        this.priority = priority;
        this.encryptedCredentials = encryptedCredentials;
    }

    public UUID getId() {
        return id;
    }

    public OrganisationConfigEntity getOrganisationConfig() {
        return organisationConfig;
    }

    public void setOrganisationConfig(OrganisationConfigEntity organisationConfig) {
        this.organisationConfig = organisationConfig;
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

    public String getEncryptedCredentials() {
        return encryptedCredentials;
    }

    public void setEncryptedCredentials(String encryptedCredentials) {
        this.encryptedCredentials = encryptedCredentials;
    }
}