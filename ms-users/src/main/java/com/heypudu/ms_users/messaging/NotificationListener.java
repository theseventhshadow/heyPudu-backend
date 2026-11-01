package com.heypudu.ms_users.messaging;

import com.heypudu.ms_users.config.amqp.RabbitTopology;
import com.heypudu.ms_users.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitTopology.NOTIFICATION_QUEUE)
    public void onNotification(NotificationEvent payload,
                               Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                               @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey)
            throws IOException {
        try {
            notificationService.createFromEvent(payload, routingKey);
            log.info("Notificacion persistida | routingKey={} | eventId={} | recipientId={}",
                    routingKey, payload.eventId(), payload.recipientId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("No se pudo procesar la notificacion | routingKey={}", routingKey, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
