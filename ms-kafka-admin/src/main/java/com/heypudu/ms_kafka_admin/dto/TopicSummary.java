package com.heypudu.ms_kafka_admin.dto;

/**
 * Resumen de un tópico para el listado administrativo.
 *
 * @param name              nombre del tópico
 * @param partitions        número de particiones
 * @param replicationFactor factor de réplica
 * @param internal          si es un tópico interno de Kafka
 */
public record TopicSummary(
        String name,
        int partitions,
        int replicationFactor,
        boolean internal) {
}
