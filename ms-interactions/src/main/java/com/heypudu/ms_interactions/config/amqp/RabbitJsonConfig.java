package com.heypudu.ms_interactions.config.amqp;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serializacion JSON de los mensajes. Spring Boot usa este converter tanto en
 * {@code RabbitTemplate} como en los {@code @RabbitListener}.
 */
@Configuration
public class RabbitJsonConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return Jackson2JsonMessageConverterFactory.create();
    }
}
