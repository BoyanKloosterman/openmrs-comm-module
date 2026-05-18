package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.RabbitMqConfig;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(NotificationQueueMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                message.getProvider().getRoutingKey(),
                message
        );
    }
}