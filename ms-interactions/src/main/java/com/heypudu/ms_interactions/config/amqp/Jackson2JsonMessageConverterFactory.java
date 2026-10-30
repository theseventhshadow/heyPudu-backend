package com.heypudu.ms_interactions.config.amqp;

import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;

/**
 * Constructor del converter JSON compartido por los microservicios de heyPudu.
 *
 * <p>El tipo se deduce del parametro del {@code @RabbitListener}, no de la
 * cabecera {@code __TypeId__}. Productor y consumidor declaran el mismo contrato
 * en paquetes distintos, asi que la cabecera apuntaria a una clase inexistente
 * en el consumidor.</p>
 */
final class Jackson2JsonMessageConverterFactory {

    private Jackson2JsonMessageConverterFactory() {
    }

    static Jackson2JsonMessageConverter create() {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(TypePrecedence.INFERRED);

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
