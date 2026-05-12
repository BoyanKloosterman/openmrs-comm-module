package nl.openmrs.comm_module.notification.controller;

import nl.openmrs.comm_module.messaging.queue.RabbitMqProducer;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import nl.openmrs.comm_module.provider.MessagingProviderType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationTestController {

    private final RabbitMqProducer rabbitMqProducer;

    public NotificationTestController(RabbitMqProducer rabbitMqProducer) {
        this.rabbitMqProducer = rabbitMqProducer;
    }

    @PostMapping("/test")
    public ResponseEntity<String> queueTestNotification(
            @RequestParam(defaultValue = "SWIFTSEND") MessagingProviderType provider
    ) {
        NotificationQueueMessage message = new NotificationQueueMessage(
                UUID.randomUUID(),
                "+31612345678",
                "Afspraak herinnering",
                "U heeft morgen om 10:00 een afspraak in kamer A12.",
                provider,
                "APPOINTMENT_REMINDER",
                Instant.now()
        );

        rabbitMqProducer.publish(message);

        return ResponseEntity.accepted().body("Notification queued for provider: " + provider);
    }
}