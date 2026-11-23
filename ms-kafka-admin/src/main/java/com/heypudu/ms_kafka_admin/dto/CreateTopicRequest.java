package com.heypudu.ms_kafka_admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos para crear un tópico.
 *
 * @param name              nombre del tópico
 * @param partitions        número de particiones (1-12)
 * @param replicationFactor factor de réplica (1-3)
 * @param retentionMs       retención en milisegundos; si es null se usa 7 días
 * @param cleanupPolicy     {@code delete} o {@code compact}
 * @param minInsyncReplicas réplicas mínimas sincronizadas
 */
public record CreateTopicRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "nombre de topico invalido")
        String name,

        @Min(1) @Max(12)
        int partitions,

        @Min(1) @Max(3)
        int replicationFactor,

        Long retentionMs,

        @Pattern(regexp = "delete|compact", message = "cleanupPolicy debe ser delete o compact")
        String cleanupPolicy,

        @Min(1) @Max(3)
        Integer minInsyncReplicas) {
}
