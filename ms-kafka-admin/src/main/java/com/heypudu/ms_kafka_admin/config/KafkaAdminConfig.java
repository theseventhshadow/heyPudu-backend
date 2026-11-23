package com.heypudu.ms_kafka_admin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente administrativo de Kafka.
 *
 * <p>Es el equivalente al {@code RabbitAdmin} del administrador de RabbitMQ,
 * pero para los objetos de Kafka: tópicos, configuraciones y grupos de consumo.</p>
 */
@Configuration
public class KafkaAdminConfig {

    @Bean
    public AdminClient kafkaAdminClient(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.CLIENT_ID_CONFIG, "ms-kafka-admin");
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 15_000);
        return AdminClient.create(props);
    }
}
