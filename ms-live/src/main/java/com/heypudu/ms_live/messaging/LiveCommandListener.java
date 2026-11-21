package com.heypudu.ms_live.messaging;

import com.heypudu.ms_live.config.amqp.RabbitTopology;
import com.heypudu.ms_live.service.LiveRoomService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consume {@code room.closed} y limpia las reacciones persistidas de la sala
 * (decisión D1). Usa ACK manual y NACK sin reencolar para terminar en la DLQ.
 */
@Component
public class LiveCommandListener {

    private static final Logger log = LoggerFactory.getLogger(LiveCommandListener.class);

    private final LiveRoomService liveRoomService;

    public LiveCommandListener(LiveRoomService liveRoomService) {
        this.liveRoomService = liveRoomService;
    }

    @RabbitListener(queues = RabbitTopology.LIVE_COMMAND_QUEUE)
    public void onRoomClosed(RoomClosedMessage message,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            liveRoomService.handleRoomClosed(message.roomId());
            channel.basicAck(deliveryTag, false);
            log.info("Reacciones de la sala {} eliminadas", message.roomId());
        } catch (Exception ex) {
            log.error("No se pudieron eliminar las reacciones de la sala {}",
                    message.roomId(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
