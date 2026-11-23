package com.heypudu.ms_kafka_admin.dto;

import java.util.List;

/**
 * Información del clúster al que está conectado el administrador.
 *
 * @param clusterId  identificador del clúster
 * @param controller broker controlador
 * @param nodes      brokers vivos
 */
public record ClusterInfo(
        String clusterId,
        int controller,
        List<Node> nodes) {

    /**
     * @param id   identificador del broker
     * @param host host anunciado
     * @param port puerto
     * @param rack rack, si está configurado
     */
    public record Node(int id, String host, int port, String rack) {
    }
}
