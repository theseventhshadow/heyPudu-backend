package com.heypudu.ms_users.messaging;

import com.heypudu.ms_users.config.amqp.RabbitListenerConfig;
import com.heypudu.ms_users.config.amqp.RabbitTopology;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class DeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

    @RabbitListener(
            queues = RabbitTopology.NOTIFICATION_QUEUE_DLQ,
            containerFactory = RabbitListenerConfig.DLQ_CONTAINER_FACTORY)
    public void onNotificationDeadLetter(
            byte[] body,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
            @Header(name = "x-first-death-reason", required = false) String reason,
            @Header(name = "x-first-death-queue", required = false) String originQueue) throws IOException {
        log.warn("MENSAJE NO ENTREGADO en {} | origen={} | routingKey={} | motivo={} | payload={}",
                RabbitTopology.NOTIFICATION_QUEUE_DLQ,
                originQueue,
                routingKey,
                reason,
                new String(body, StandardCharsets.UTF_8));
        channel.basicAck(deliveryTag, false);
    }
}
