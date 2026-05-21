package nl.openmrs.comm_module.notification.reminder;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lookup van geregistreerde herinneringsspecs (SRP: alleen catalogus). */
@Component
public class AppointmentReminderCatalog {

    private final List<AppointmentReminderSpec> ordered;
    private final Map<String, AppointmentReminderSpec> byId;
    private final Map<String, AppointmentReminderSpec> byMessageType;

    public AppointmentReminderCatalog(List<AppointmentReminderSpec> specs) {
        this.ordered = List.copyOf(specs);
        this.byId = specs.stream().collect(Collectors.toMap(AppointmentReminderSpec::id, Function.identity()));
        this.byMessageType =
                specs.stream().collect(Collectors.toMap(AppointmentReminderSpec::messageType, Function.identity()));
    }

    public List<AppointmentReminderSpec> all() {
        return ordered;
    }

    public Optional<AppointmentReminderSpec> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<AppointmentReminderSpec> findByMessageType(String messageType) {
        return Optional.ofNullable(byMessageType.get(messageType));
    }
}
