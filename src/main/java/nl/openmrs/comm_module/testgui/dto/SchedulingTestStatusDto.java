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
        /** Zone van naive start_date_time in MariaDB (meestal UTC bij reference distro). */
        String appointmentDbZoneId,
        /** Zone voor weergave van opgeslagen Instant (notificaties + test-GUI). */
        String appointmentDisplayZoneId,
        String defaultProvider,
        List<String> availableProviders,
        ReminderWindowDto reminderWindow,
        int appointmentsCurrentlyInWindow,
        ReminderWindowDto oneHourReminderWindow,
        int appointmentsCurrentlyIn1hWindow,
        PollDiagnosticsDto lastPoll) {}
