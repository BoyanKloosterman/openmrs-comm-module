package nl.openmrs.comm_module.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PROVIDER_EXCHANGE = "provider.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "provider.dead-letter.exchange";

    public static final String SWIFTSEND_QUEUE = "queue.swiftsend";
    public static final String SECUREPOST_QUEUE = "queue.securepost";
    public static final String LEGACYLINK_QUEUE = "queue.legacylink";
    public static final String ASYNCFLOW_QUEUE = "queue.asyncflow";

    public static final String SWIFTSEND_DLQ = "dlq.swiftsend";
    public static final String SECUREPOST_DLQ = "dlq.securepost";
    public static final String LEGACYLINK_DLQ = "dlq.legacylink";
    public static final String ASYNCFLOW_DLQ = "dlq.asyncflow";

    public static final String SWIFTSEND_ROUTING_KEY = "provider.swiftsend";
    public static final String SECUREPOST_ROUTING_KEY = "provider.securepost";
    public static final String LEGACYLINK_ROUTING_KEY = "provider.legacylink";
    public static final String ASYNCFLOW_ROUTING_KEY = "provider.asyncflow";

    public static final String SWIFTSEND_DLQ_ROUTING_KEY = "dlq.swiftsend";
    public static final String SECUREPOST_DLQ_ROUTING_KEY = "dlq.securepost";
    public static final String LEGACYLINK_DLQ_ROUTING_KEY = "dlq.legacylink";
    public static final String ASYNCFLOW_DLQ_ROUTING_KEY = "dlq.asyncflow";

    public static final String RETRY_EXCHANGE = "notification.retry.exchange";

    public static final String SWIFTSEND_RETRY_QUEUE = "retry.swiftsend";
    public static final String SECUREPOST_RETRY_QUEUE = "retry.securepost";
    public static final String LEGACYLINK_RETRY_QUEUE = "retry.legacylink";
    public static final String ASYNCFLOW_RETRY_QUEUE = "retry.asyncflow";

    @Bean
    public DirectExchange providerExchange() {
        return new DirectExchange(PROVIDER_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(RETRY_EXCHANGE);
    }

    @Bean
    public Queue swiftSendQueue() {
        return QueueBuilder.durable(SWIFTSEND_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(SWIFTSEND_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue securePostQueue() {
        return QueueBuilder.durable(SECUREPOST_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(SECUREPOST_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue legacyLinkQueue() {
        return QueueBuilder.durable(LEGACYLINK_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(LEGACYLINK_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue asyncFlowQueue() {
        return QueueBuilder.durable(ASYNCFLOW_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ASYNCFLOW_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean("swiftSendRetryQueue")
    public Queue swiftSendRetryQueue() {
        return QueueBuilder.durable(SWIFTSEND_RETRY_QUEUE)
                .deadLetterExchange(PROVIDER_EXCHANGE)
                .deadLetterRoutingKey(SWIFTSEND_ROUTING_KEY)
                .build();
    }

    @Bean("securePostRetryQueue")
    public Queue securePostRetryQueue() {
        return QueueBuilder.durable(SECUREPOST_RETRY_QUEUE)
                .deadLetterExchange(PROVIDER_EXCHANGE)
                .deadLetterRoutingKey(SECUREPOST_ROUTING_KEY)
                .build();
    }

    @Bean("legacyLinkRetryQueue")
    public Queue legacyLinkRetryQueue() {
        return QueueBuilder.durable(LEGACYLINK_RETRY_QUEUE)
                .deadLetterExchange(PROVIDER_EXCHANGE)
                .deadLetterRoutingKey(LEGACYLINK_ROUTING_KEY)
                .build();
    }

    @Bean("asyncFlowRetryQueue")
    public Queue asyncFlowRetryQueue() {
        return QueueBuilder.durable(ASYNCFLOW_RETRY_QUEUE)
                .deadLetterExchange(PROVIDER_EXCHANGE)
                .deadLetterRoutingKey(ASYNCFLOW_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue swiftSendDeadLetterQueue() {
        return QueueBuilder.durable(SWIFTSEND_DLQ).build();
    }

    @Bean
    public Queue securePostDeadLetterQueue() {
        return QueueBuilder.durable(SECUREPOST_DLQ).build();
    }

    @Bean
    public Queue legacyLinkDeadLetterQueue() {
        return QueueBuilder.durable(LEGACYLINK_DLQ).build();
    }

    @Bean
    public Queue asyncFlowDeadLetterQueue() {
        return QueueBuilder.durable(ASYNCFLOW_DLQ).build();
    }



    @Bean
    public Binding swiftSendBinding() {
        return BindingBuilder
                .bind(swiftSendQueue())
                .to(providerExchange())
                .with(SWIFTSEND_ROUTING_KEY);
    }

    @Bean
    public Binding securePostBinding() {
        return BindingBuilder
                .bind(securePostQueue())
                .to(providerExchange())
                .with(SECUREPOST_ROUTING_KEY);
    }

    @Bean
    public Binding legacyLinkBinding() {
        return BindingBuilder
                .bind(legacyLinkQueue())
                .to(providerExchange())
                .with(LEGACYLINK_ROUTING_KEY);
    }

    @Bean
    public Binding asyncFlowBinding() {
        return BindingBuilder
                .bind(asyncFlowQueue())
                .to(providerExchange())
                .with(ASYNCFLOW_ROUTING_KEY);
    }

    @Bean
    public Binding swiftSendRetryBinding(Queue swiftSendRetryQueue) {
        return BindingBuilder
                .bind(swiftSendRetryQueue)
                .to(retryExchange())
                .with(SWIFTSEND_ROUTING_KEY);
    }

    @Bean
    public Binding securePostRetryBinding(Queue securePostRetryQueue) {
        return BindingBuilder
                .bind(securePostRetryQueue)
                .to(retryExchange())
                .with(SECUREPOST_ROUTING_KEY);
    }

    @Bean
    public Binding legacyLinkRetryBinding(Queue legacyLinkRetryQueue) {
        return BindingBuilder
                .bind(legacyLinkRetryQueue)
                .to(retryExchange())
                .with(LEGACYLINK_ROUTING_KEY);
    }

    @Bean
    public Binding asyncFlowRetryBinding(Queue asyncFlowRetryQueue) {
        return BindingBuilder
                .bind(asyncFlowRetryQueue)
                .to(retryExchange())
                .with(ASYNCFLOW_ROUTING_KEY);
    }

    @Bean
    public Binding swiftSendDeadLetterBinding() {
        return BindingBuilder
                .bind(swiftSendDeadLetterQueue())
                .to(deadLetterExchange())
                .with(SWIFTSEND_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding securePostDeadLetterBinding() {
        return BindingBuilder
                .bind(securePostDeadLetterQueue())
                .to(deadLetterExchange())
                .with(SECUREPOST_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding legacyLinkDeadLetterBinding() {
        return BindingBuilder
                .bind(legacyLinkDeadLetterQueue())
                .to(deadLetterExchange())
                .with(LEGACYLINK_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding asyncFlowDeadLetterBinding() {
        return BindingBuilder
                .bind(asyncFlowDeadLetterQueue())
                .to(deadLetterExchange())
                .with(ASYNCFLOW_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}