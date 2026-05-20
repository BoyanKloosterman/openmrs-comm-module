package nl.openmrs.comm_module.testgui.dto;

import java.time.Instant;

public record ReminderWindowDto(
        Instant target,
        Instant windowStart,
        Instant windowEnd,
        int leadHours,
        int windowMinutes) {}
