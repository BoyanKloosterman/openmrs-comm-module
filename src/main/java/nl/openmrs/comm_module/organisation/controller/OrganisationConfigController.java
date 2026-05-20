package nl.openmrs.comm_module.organisation.controller;

import nl.openmrs.comm_module.organisation.dto.OrganisationConfigRequest;
import nl.openmrs.comm_module.organisation.dto.OrganisationConfigResponse;
import nl.openmrs.comm_module.organisation.service.OrganisationConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organisations/config")
public class OrganisationConfigController {

    private final OrganisationConfigService organisationConfigService;

    public OrganisationConfigController(OrganisationConfigService organisationConfigService) {
        this.organisationConfigService = organisationConfigService;
    }

    @PostMapping
    public ResponseEntity<OrganisationConfigResponse> saveConfig(
            @RequestBody OrganisationConfigRequest request
    ) {
        OrganisationConfigResponse response = organisationConfigService.saveConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{organisationId}")
    public ResponseEntity<OrganisationConfigResponse> getConfig(
            @PathVariable String organisationId
    ) {
        OrganisationConfigResponse response = organisationConfigService.getConfig(organisationId);
        return ResponseEntity.ok(response);
    }
}