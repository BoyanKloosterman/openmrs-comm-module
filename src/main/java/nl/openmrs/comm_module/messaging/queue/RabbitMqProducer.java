package nl.openmrs.comm_module.messaging.queue;

import nl.openmrs.comm_module.config.RabbitMqConfig;
import nl.openmrs.comm_module.messaging.queue.dto.NotificationQueueMessage;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(NotificationQueueMessage message) {
        sendToProviderQueue(message);
    }

    public void publishRetry(NotificationQueueMessage message, long delayMs) {
        MessagePostProcessor delayProcessor = msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(delayMs));
            return msg;
        };

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RETRY_EXCHANGE,
                message.getProvider().getRoutingKey(),
                message,
                delayProcessor
        );
    }

    private void sendToProviderQueue(NotificationQueueMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                message.getProvider().getRoutingKey(),
                message
        );
    }
}