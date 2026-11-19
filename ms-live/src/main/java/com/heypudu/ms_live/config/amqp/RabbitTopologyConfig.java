package com.heypudu.ms_live.config.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.heypudu.ms_live.config.amqp.RabbitTopology.COMMAND_EXCHANGE;
import static com.heypudu.ms_live.config.amqp.RabbitTopology.DLX_EXCHANGE;
import static com.heypudu.ms_live.config.amqp.RabbitTopology.EVENTS_EXCHANGE;
import static com.heypudu.ms_live.config.amqp.RabbitTopology.LIVE_COMMAND_QUEUE;
import static com.heypudu.ms_live.config.amqp.RabbitTopology.LIVE_COMMAND_QUEUE_DLQ;
import static com.heypudu.ms_live.config.amqp.RabbitTopology.RK_ROOM_CLOSED;

/**
 * Topología RabbitMQ de {@code ms-live}.
 *
 * <ul>
 *   <li>{@code ex.heypudu.events}: publica {@code room.created}.</li>
 *   <li>{@code ex.heypudu.cmd}: recibe {@code room.closed} en su propia cola.</li>
 *   <li>{@code ex.heypudu.dlx}: recibe los mensajes rechazados.</li>
 * </ul>
 */
@Configuration
public class RabbitTopologyConfig {

    private final int messageTtlMs;
    private final int maxLength;
    private final int deliveryLimit;

    public RabbitTopologyConfig(
            @Value("${rabbit.topology.message-ttl-ms:3600000}") int messageTtlMs,
            @Value("${rabbit.topology.max-length:10000}") int maxLength,
            @Value("${rabbit.topology.delivery-limit:3}") int deliveryLimit) {
        this.messageTtlMs = messageTtlMs;
        this.maxLength = maxLength;
        this.deliveryLimit = deliveryLimit;
    }

    @Bean
    public DirectExchange commandExchange() {
        return new DirectExchange(COMMAND_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue liveCommandQueue() {
        return QueueBuilder.durable(LIVE_COMMAND_QUEUE)
                .quorum()
                .ttl(messageTtlMs)
                .maxLength(maxLength)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .deliveryLimit(deliveryLimit)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(LIVE_COMMAND_QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue liveCommandDlq() {
        return QueueBuilder.durable(LIVE_COMMAND_QUEUE_DLQ)
                .quorum()
                .maxLength(maxLength)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .build();
    }

    @Bean
    public Binding liveCommandBinding() {
        return BindingBuilder.bind(liveCommandQueue()).to(commandExchange()).with(RK_ROOM_CLOSED);
    }

    @Bean
    public Binding liveCommandDlqBinding() {
        return BindingBuilder.bind(liveCommandDlq()).to(deadLetterExchange()).with(LIVE_COMMAND_QUEUE_DLQ);
    }
}
