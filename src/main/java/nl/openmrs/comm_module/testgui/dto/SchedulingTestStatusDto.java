package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

public record SchedulingTestStatusDto(
        Instant serverTime,
        String organisationId,
        boolean schedulerEnabled,
        int reminderLeadHours,
        int reminderWindowMinutes,
        int schedulerCheckIntervalMinutes,
        int fhirPollIntervalMinutes,
        String reminderZoneId,
        String defaultProvider,
        ReminderWindowDto reminderWindow,
        int appointmentsCurrentlyInWindow) {}
