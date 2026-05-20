package nl.openmrs.comm_module.testgui.dto;

public record OpenmrsPatientOptionDto(
        String patientUuid,
        String displayName,
        String phoneMasked,
        boolean hasPhone) {}
