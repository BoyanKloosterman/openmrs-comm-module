package nl.openmrs.comm_module.persistence.dao;

import nl.openmrs.comm_module.persistence.entity.OrganisationProviderConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganisationProviderConfigRepository extends JpaRepository<OrganisationProviderConfigEntity, UUID> {

    List<OrganisationProviderConfigEntity> findByOrganisationConfigOrganisationIdAndEnabledTrueOrderByPriorityAsc(
            String organisationId
    );
}