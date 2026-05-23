package nl.openmrs.comm_module.testgui.dto;

public record TriggerResultDto(boolean success, String message, PollDiagnosticsDto diagnostics) {}
