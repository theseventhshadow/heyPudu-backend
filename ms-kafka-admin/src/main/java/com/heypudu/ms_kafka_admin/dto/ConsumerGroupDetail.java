package com.heypudu.ms_kafka_admin.dto;

import java.util.List;

/**
 * Estado de un grupo de consumidores.
 *
 * @param groupId grupo
 * @param state   estado reportado por el broker (Stable, Empty, Dead…)
 * @param members miembros activos
 * @param topics  particiones asignadas con su LAG
 */
public record ConsumerGroupDetail(
        String groupId,
        String state,
        List<Member> members,
        List<PartitionLag> partitions) {

    /**
     * @param memberId identificador del miembro
     * @param clientId cliente que se unió al grupo
     * @param host     host del consumidor
     */
    public record Member(String memberId, String clientId, String host) {
    }

    /**
     * @param topic           tópico
     * @param partition       partición
     * @param currentOffset   offset confirmado por el grupo (-1 si no hay)
     * @param logEndOffset    último offset del tópico
     * @param lag             diferencia entre ambos
     */
    public record PartitionLag(
            String topic,
            int partition,
            long currentOffset,
            long logEndOffset,
            long lag) {
    }
}
