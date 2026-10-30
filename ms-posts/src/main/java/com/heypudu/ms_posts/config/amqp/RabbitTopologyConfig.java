package com.heypudu.ms_posts.config.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.heypudu.ms_posts.config.amqp.RabbitTopology.AUDIT_QUEUE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.AUDIT_QUEUE_DLQ;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.COMMAND_EXCHANGE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.COMMAND_QUEUE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.COMMAND_QUEUE_DLQ;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.DLX_EXCHANGE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.EVENTS_EXCHANGE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.NOTIFICATION_QUEUE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.NOTIFICATION_QUEUE_DLQ;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.RK_AUDIT_FOLLOW_PATTERN;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.RK_AUDIT_PATTERN;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.RK_INTERACTION_COMMENT;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.RK_INTERACTION_LIKE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.RK_POST_DELETE;
import static com.heypudu.ms_posts.config.amqp.RabbitTopology.RK_USER_FOLLOWED;

/**
 * Topologia de RabbitMQ de heyPudu.
 *
 * <p>Los nombres viven en {@link RabbitTopology}; aqui solo se declaran los
 * objetos. Spring AMQP los crea de forma idempotente al abrir la conexion.</p>
 *
 * <ul>
 *   <li>{@code ex.heypudu.cmd} (direct): comandos con destino unico.</li>
 *   <li>{@code ex.heypudu.events} (topic): eventos que interesan a varios
 *       consumidores, enrutados por patron.</li>
 *   <li>{@code ex.heypudu.dlx} (direct): recibe los mensajes rechazados.</li>
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

    // ============================================================
    // Exchanges
    // ============================================================

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

    // ============================================================
    // Colas funcionales, cada una con su DLQ
    // ============================================================

    @Bean
    public Queue commandQueue() {
        return durableQueue(COMMAND_QUEUE, COMMAND_QUEUE_DLQ);
    }

    @Bean
    public Queue notificationQueue() {
        return durableQueue(NOTIFICATION_QUEUE, NOTIFICATION_QUEUE_DLQ);
    }

    @Bean
    public Queue auditQueue() {
        return durableQueue(AUDIT_QUEUE, AUDIT_QUEUE_DLQ);
    }

    @Bean
    public Queue commandDlq() {
        return deadLetterQueue(COMMAND_QUEUE_DLQ);
    }

    @Bean
    public Queue notificationDlq() {
        return deadLetterQueue(NOTIFICATION_QUEUE_DLQ);
    }

    @Bean
    public Queue auditDlq() {
        return deadLetterQueue(AUDIT_QUEUE_DLQ);
    }

    // ============================================================
    // Bindings
    // ============================================================

    @Bean
    public Binding commandBinding() {
        return BindingBuilder.bind(commandQueue()).to(commandExchange()).with(RK_POST_DELETE);
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
    public Binding auditInteractionBinding() {
        return BindingBuilder.bind(auditQueue()).to(eventsExchange()).with(RK_AUDIT_PATTERN);
    }

    @Bean
    public Binding auditFollowBinding() {
        return BindingBuilder.bind(auditQueue()).to(eventsExchange()).with(RK_AUDIT_FOLLOW_PATTERN);
    }

    @Bean
    public Binding commandDlqBinding() {
        return BindingBuilder.bind(commandDlq()).to(deadLetterExchange()).with(COMMAND_QUEUE_DLQ);
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(deadLetterExchange()).with(NOTIFICATION_QUEUE_DLQ);
    }

    @Bean
    public Binding auditDlqBinding() {
        return BindingBuilder.bind(auditDlq()).to(deadLetterExchange()).with(AUDIT_QUEUE_DLQ);
    }

    // ============================================================
    // Construccion de colas
    // ============================================================

    private Queue durableQueue(String name, String dlqName) {
        return QueueBuilder.durable(name)
                .quorum()
                .ttl(messageTtlMs)
                .maxLength(maxLength)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .deliveryLimit(deliveryLimit)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(dlqName)
                .build();
    }

    private Queue deadLetterQueue(String name) {
        return QueueBuilder.durable(name)
                .quorum()
                .maxLength(maxLength)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .build();
    }
}
