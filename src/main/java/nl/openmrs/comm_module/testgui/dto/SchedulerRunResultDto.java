package nl.openmrs.comm_module.testgui.dto;

public record SchedulerRunResultDto(
        boolean success,
        int dueBeforeRun,
        int dueAfterRun,
        int queued,
        String provider,
        String message) {}
