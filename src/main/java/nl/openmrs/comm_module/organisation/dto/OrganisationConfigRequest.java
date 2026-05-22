package nl.openmrs.comm_module.organisation.dto;

import java.util.ArrayList;
import java.util.List;

public class OrganisationConfigRequest {

  private String organisationId;
  private boolean active = true;
  private String timezone = "Europe/Amsterdam";
  private List<OrganisationProviderConfigRequest> providers = new ArrayList<>();

  public OrganisationConfigRequest() {
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

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public List<OrganisationProviderConfigRequest> getProviders() {
    return providers;
  }

  public void setProviders(List<OrganisationProviderConfigRequest> providers) {
    this.providers = providers;
  }
}