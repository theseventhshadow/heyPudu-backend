package com.heypudu.ms_posts.messaging;

import com.heypudu.ms_posts.config.amqp.RabbitTopology;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    @RabbitListener(queues = RabbitTopology.AUDIT_QUEUE)
    public void onAuditEvent(AuditEvent event,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                             @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false)
                             String routingKey) throws IOException {
        try {
            log.info("AUDIT evento recibido | routingKey={} | eventId={} | actorId={} | recipientId={} | postId={} | commentId={}",
                    routingKey,
                    event.eventId(),
                    event.actorId(),
                    event.recipientId(),
                    event.postId(),
                    event.commentId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("No se pudo procesar el evento de auditoria | routingKey={}", routingKey, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
