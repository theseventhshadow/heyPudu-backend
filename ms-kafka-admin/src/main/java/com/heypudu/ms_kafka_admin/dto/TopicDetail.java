package com.heypudu.ms_kafka_admin.dto;

import java.util.List;
import java.util.Map;

/**
 * Detalle de un tópico: particiones con sus réplicas e ISR, más sus configuraciones.
 *
 * @param name              nombre del tópico
 * @param internal          si es interno
 * @param partitions        particiones con líder, réplicas e ISR
 * @param configs           configuraciones efectivas del tópico
 */
public record TopicDetail(
        String name,
        boolean internal,
        List<PartitionInfo> partitions,
        Map<String, String> configs) {

    /**
     * @param id       número de partición
     * @param leader   broker líder
     * @param replicas brokers con réplica
     * @param isr      réplicas sincronizadas
     */
    public record PartitionInfo(
            int id,
            int leader,
            List<Integer> replicas,
            List<Integer> isr) {
    }
}
