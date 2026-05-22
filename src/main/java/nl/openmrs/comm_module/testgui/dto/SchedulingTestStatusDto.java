package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;
import java.util.List;

public record SchedulingTestStatusDto(
        Instant serverTime,
        String organisationId,
        String fhirServerUrl,
        boolean fhirSyncEnabled,
        int appointmentPollSinceDays,
        Instant pollWindowFrom,
        Instant pollWindowTo,
        boolean schedulerEnabled,
        int reminderLeadHours,
        int reminder1LeadHours,
        int reminderWindowMinutes,
        int schedulerCheckIntervalMinutes,
        int fhirPollIntervalMinutes,
        String reminderZoneId,
        String defaultProvider,
        List<String> availableProviders,
        ReminderWindowDto reminderWindow,
        int appointmentsCurrentlyInWindow,
        ReminderWindowDto oneHourReminderWindow,
        int appointmentsCurrentlyIn1hWindow,
        PollDiagnosticsDto lastPoll) {}
