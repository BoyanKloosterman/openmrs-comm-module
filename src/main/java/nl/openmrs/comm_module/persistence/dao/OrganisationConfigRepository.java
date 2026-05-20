package nl.openmrs.comm_module.persistence.dao;

import nl.openmrs.comm_module.persistence.entity.OrganisationConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationConfigRepository extends JpaRepository<OrganisationConfigEntity, UUID> {

    Optional<OrganisationConfigEntity> findByOrganisationId(String organisationId);

    boolean existsByOrganisationId(String organisationId);
}