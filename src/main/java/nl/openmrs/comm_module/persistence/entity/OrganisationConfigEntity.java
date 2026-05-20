package nl.openmrs.comm_module.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "organisation_config")
public class OrganisationConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, unique = true)
    private String organisationId;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(
            mappedBy = "organisationConfig",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrganisationProviderConfigEntity> providers = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OrganisationConfigEntity() {
    }

    public OrganisationConfigEntity(String organisationId, boolean active) {
        this.organisationId = organisationId;
        this.active = active;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addProvider(OrganisationProviderConfigEntity providerConfig) {
        providers.add(providerConfig);
        providerConfig.setOrganisationConfig(this);
    }

    public void removeProvider(OrganisationProviderConfigEntity providerConfig) {
        providers.remove(providerConfig);
        providerConfig.setOrganisationConfig(null);
    }

    public UUID getId() {
        return id;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<OrganisationProviderConfigEntity> getProviders() {
        return providers;
    }

    public void setProviders(List<OrganisationProviderConfigEntity> providers) {
        this.providers = providers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}