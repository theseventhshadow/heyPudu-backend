package com.heypudu.ms_interactions.messaging;

import com.heypudu.ms_interactions.config.amqp.RabbitListenerConfig;
import com.heypudu.ms_interactions.config.amqp.RabbitTopology;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Consume las tres colas de mensajes no entregados y deja constancia en el log.
 *
 * <p>Es la evidencia de la EP2: un mensaje que no pudo procesarse termina en la
 * DLQ de su cola y aqui queda registrado con su origen y su contenido.</p>
 *
 * <p>El cuerpo se recibe como {@code byte[]} a proposito: un mensaje llega a la
 * DLQ justamente porque no se pudo interpretar, asi que no se puede pedir a
 * Spring que lo deserialice a un tipo concreto.</p>
 */
@Component
public class DeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

    @RabbitListener(queues = RabbitTopology.COMMAND_QUEUE_DLQ, containerFactory = RabbitListenerConfig.DLQ_CONTAINER_FACTORY)
    public void onCommandDeadLetter(byte[] body,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                    @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
                                    @Header(name = "x-first-death-reason", required = false) String reason,
                                    @Header(name = "x-first-death-queue", required = false) String originQueue) throws IOException {
        logDeadLetter(RabbitTopology.COMMAND_QUEUE_DLQ, body, routingKey, reason, originQueue);
        channel.basicAck(deliveryTag, false);
    }

    @RabbitListener(queues = RabbitTopology.NOTIFICATION_QUEUE_DLQ, containerFactory = RabbitListenerConfig.DLQ_CONTAINER_FACTORY)
    public void onNotificationDeadLetter(byte[] body,
                                         Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                         @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
                                         @Header(name = "x-first-death-reason", required = false) String reason,
                                         @Header(name = "x-first-death-queue", required = false) String originQueue) throws IOException {
        logDeadLetter(RabbitTopology.NOTIFICATION_QUEUE_DLQ, body, routingKey, reason, originQueue);
        channel.basicAck(deliveryTag, false);
    }

    @RabbitListener(queues = RabbitTopology.AUDIT_QUEUE_DLQ, containerFactory = RabbitListenerConfig.DLQ_CONTAINER_FACTORY)
    public void onAuditDeadLetter(byte[] body,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
                                  @Header(name = "x-first-death-reason", required = false) String reason,
                                  @Header(name = "x-first-death-queue", required = false) String originQueue) throws IOException {
        logDeadLetter(RabbitTopology.AUDIT_QUEUE_DLQ, body, routingKey, reason, originQueue);
        channel.basicAck(deliveryTag, false);
    }

    private void logDeadLetter(String dlq, byte[] body, String routingKey, String reason, String originQueue) {
        log.warn("MENSAJE NO ENTREGADO en {} | origen={} | routingKey={} | motivo={} | payload={}",
                dlq, originQueue, routingKey, reason, new String(body, StandardCharsets.UTF_8));
    }
}
