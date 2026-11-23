package com.heypudu.ms_analytics.config;

import com.heypudu.ms_analytics.messaging.LiveRoomEvent;
import com.heypudu.ms_analytics.messaging.PostEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private final DefaultErrorHandler kafkaErrorHandler;

    public KafkaConsumerConfig(DefaultErrorHandler kafkaErrorHandler) {
        this.kafkaErrorHandler = kafkaErrorHandler;
    }

    @Bean
    public ConsumerFactory<String, LiveRoomEvent> liveEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:live-analytics-group}") String groupId) {
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(bootstrapServers, groupId, LiveRoomEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LiveRoomEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, LiveRoomEvent> liveEventConsumerFactory) {
        return manualAckFactory(liveEventConsumerFactory);
    }

    /**
     * Segundo tópico: publicaciones. Tiene su propio DTO y su propio grupo, así
     * que necesita su propio contenedor.
     */
    @Bean
    public ConsumerFactory<String, PostEvent> postEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${posts.analytics.group-id:posts-analytics-group}") String groupId) {
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(bootstrapServers, groupId, PostEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostEvent> postsListenerContainerFactory(
            ConsumerFactory<String, PostEvent> postEventConsumerFactory) {
        return manualAckFactory(postEventConsumerFactory);
    }

    private Map<String, Object> consumerProps(String bootstrapServers, String groupId,
                                              Class<?> valueType) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // El consumidor fija el DTO destino; el productor no envía cabeceras de tipo.
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.heypudu.ms_analytics.messaging");
        return props;
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> manualAckFactory(
            ConsumerFactory<String, T> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Se confirma cada registro procesado; los fallos pasan por el
        // DefaultErrorHandler (reintentos + DLT).
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
