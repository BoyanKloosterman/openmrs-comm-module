package nl.openmrs.comm_module.testgui.dto;

/** Welke herinnering de test-GUI bedient (24u, 1u of beide). */
public enum ReminderKind {
    H24,
    H1,
    ALL;

    public static ReminderKind parse(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.trim().toUpperCase()) {
            case "24", "24H", "H24" -> H24;
            case "1", "1H", "H1" -> H1;
            case "ALL", "BOTH" -> ALL;
            default -> throw new IllegalArgumentException("Onbekend reminder-type: " + value);
        };
    }
}
