package com.heypudu.ms_users.config.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitJsonConfig {

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(TypePrecedence.INFERRED);

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
