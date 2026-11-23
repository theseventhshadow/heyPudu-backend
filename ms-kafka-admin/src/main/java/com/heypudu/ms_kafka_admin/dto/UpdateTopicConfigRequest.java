package com.heypudu.ms_kafka_admin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * Configuraciones a modificar en un tópico existente.
 *
 * <p>Se aplican de forma incremental: solo cambian las claves enviadas.</p>
 *
 * @param configs pares clave-valor, por ejemplo {@code retention.ms} o {@code min.insync.replicas}
 */
public record UpdateTopicConfigRequest(
        @NotEmpty(message = "debe enviar al menos una configuracion")
        Map<String, String> configs) {
}
