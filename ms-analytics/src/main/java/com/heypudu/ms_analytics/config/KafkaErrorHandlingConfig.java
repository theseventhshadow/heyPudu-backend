package com.heypudu.ms_analytics.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.common.TopicPartition;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejo de errores de los consumidores: reintentos con backoff y DLT.
 *
 * <p>Comportamiento:</p>
 * <ol>
 *   <li>Un payload inválido ({@link IllegalArgumentException}) no se reintenta:
 *       va directo al tópico {@code <original>.DLT}.</li>
 *   <li>Cualquier otro fallo se reintenta 3 veces con backoff exponencial
 *       (500 ms, 1 s, 2 s).</li>
 *   <li>Si se agotan los reintentos, el mensaje se publica en
 *       {@code <original>.DLT} y se confirma el offset, de modo que un mensaje
 *       defectuoso no bloquea la partición.</li>
 * </ol>
 */
@Configuration
public class KafkaErrorHandlingConfig {

    /** Sufijo de los tópicos de mensajes no procesables. */
    public static final String DLT_SUFFIX = ".DLT";

    @Bean
    public ProducerFactory<String, Object> deadLetterProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        JsonSerializer<Object> serializer = new JsonSerializer<>();
        serializer.setAddTypeInfo(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), serializer);
    }

    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate(
            ProducerFactory<String, Object> deadLetterProducerFactory) {
        return new KafkaTemplate<>(deadLetterProducerFactory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> deadLetterKafkaTemplate) {
        // Publica en <topico>.DLT manteniendo la partición original; el sufijo
        // por defecto de Spring Kafka es "-dlt", y aquí se usa ".DLT".
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deadLetterKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));
        recoverer.setFailIfSendResultIsError(true);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(500L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(4_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class, DeserializationException.class);
        handler.setCommitRecovered(true);
        return handler;
    }
}
