package com.heypudu.ms_users.config.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.heypudu.ms_users.config.amqp.RabbitTopology.DLX_EXCHANGE;
import static com.heypudu.ms_users.config.amqp.RabbitTopology.EVENTS_EXCHANGE;
import static com.heypudu.ms_users.config.amqp.RabbitTopology.NOTIFICATION_QUEUE;
import static com.heypudu.ms_users.config.amqp.RabbitTopology.NOTIFICATION_QUEUE_DLQ;
import static com.heypudu.ms_users.config.amqp.RabbitTopology.RK_INTERACTION_COMMENT;
import static com.heypudu.ms_users.config.amqp.RabbitTopology.RK_INTERACTION_LIKE;
import static com.heypudu.ms_users.config.amqp.RabbitTopology.RK_USER_FOLLOWED;

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
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .quorum()
                .ttl(messageTtlMs)
                .maxLength(maxLength)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .deliveryLimit(deliveryLimit)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(NOTIFICATION_QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE_DLQ)
                .quorum()
                .maxLength(maxLength)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .build();
    }

    @Bean
    public Binding notificationLikeBinding() {
        return BindingBuilder.bind(notificationQueue()).to(eventsExchange()).with(RK_INTERACTION_LIKE);
    }

    @Bean
    public Binding notificationCommentBinding() {
        return BindingBuilder.bind(notificationQueue()).to(eventsExchange()).with(RK_INTERACTION_COMMENT);
    }

    @Bean
    public Binding notificationFollowBinding() {
        return BindingBuilder.bind(notificationQueue()).to(eventsExchange()).with(RK_USER_FOLLOWED);
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(deadLetterExchange()).with(NOTIFICATION_QUEUE_DLQ);
    }
}
