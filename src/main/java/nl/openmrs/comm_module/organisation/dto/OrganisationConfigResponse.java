package nl.openmrs.comm_module.organisation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrganisationConfigResponse {

    private UUID id;
    private String organisationId;
    private boolean active;
    private List<OrganisationProviderConfigResponse> providers;
    private Instant createdAt;
    private Instant updatedAt;

    public OrganisationConfigResponse(
            UUID id,
            String organisationId,
            boolean active,
            List<OrganisationProviderConfigResponse> providers,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.organisationId = organisationId;
        this.active = active;
        this.providers = providers;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public boolean isActive() {
        return active;
    }

    public List<OrganisationProviderConfigResponse> getProviders() {
        return providers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}