package com.heypudu.ms_interactions.config.amqp;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de escucha.
 *
 * <p>Las colas de mensajes no entregados usan un factory propio con
 * {@link SimpleMessageConverter}: un mensaje llega a la DLQ precisamente porque
 * no se pudo interpretar, asi que el converter JSON fallaria otra vez al leerlo
 * y el mensaje quedaria atrapado sin registrarse.</p>
 */
@Configuration
public class RabbitListenerConfig {

    public static final String DLQ_CONTAINER_FACTORY = "dlqContainerFactory";

    @Bean(DLQ_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory dlqContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
