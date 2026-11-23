package com.heypudu.ms_kafka_admin.service.impl;

import com.heypudu.ms_kafka_admin.dto.ClusterInfo;
import com.heypudu.ms_kafka_admin.dto.ConsumerGroupDetail;
import com.heypudu.ms_kafka_admin.dto.CreateTopicRequest;
import com.heypudu.ms_kafka_admin.dto.TopicDetail;
import com.heypudu.ms_kafka_admin.dto.TopicSummary;
import com.heypudu.ms_kafka_admin.dto.UpdateTopicConfigRequest;
import com.heypudu.ms_kafka_admin.exception.BusinessException;
import com.heypudu.ms_kafka_admin.exception.ResourceNotFoundException;
import com.heypudu.ms_kafka_admin.service.KafkaAdminService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class KafkaAdminServiceImpl implements KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminServiceImpl.class);
    private static final long TIMEOUT_SECONDS = 15;

    private final AdminClient adminClient;

    public KafkaAdminServiceImpl(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public ClusterInfo describeCluster() {
        try {
            var cluster = adminClient.describeCluster();
            List<ClusterInfo.Node> nodes = cluster.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .stream()
                    .map(node -> new ClusterInfo.Node(node.id(), node.host(), node.port(), node.rack()))
                    .sorted(Comparator.comparingInt(ClusterInfo.Node::id))
                    .toList();
            var controller = cluster.controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String clusterId = cluster.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new ClusterInfo(clusterId, controller != null ? controller.id() : -1, nodes);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Consulta interrumpida al cluster de Kafka");
        } catch (ExecutionException | TimeoutException ex) {
            throw new BusinessException("No se pudo consultar el cluster de Kafka: " + rootMessage(ex));
        }
    }

    @Override
    public List<TopicSummary> listTopics() {
        try {
            Set<String> names = adminClient.listTopics().names().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Map<String, TopicDescription> descriptions =
                    adminClient.describeTopics(names).allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return descriptions.values().stream()
                    .map(description -> new TopicSummary(
                            description.name(),
                            description.partitions().size(),
                            description.partitions().isEmpty()
                                    ? 0
                                    : description.partitions().get(0).replicas().size(),
                            description.isInternal()))
                    .sorted(Comparator.comparing(TopicSummary::name))
                    .toList();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Consulta interrumpida al cluster de Kafka");
        } catch (ExecutionException | TimeoutException ex) {
            throw new BusinessException("No se pudieron listar los topicos: " + rootMessage(ex));
        }
    }

    @Override
    public TopicDetail describeTopic(String topic) {
        return readTopic(topic);
    }

    @Override
    public void createTopic(CreateTopicRequest request) {
        Map<String, String> configs = new HashMap<>();
        configs.put("retention.ms", String.valueOf(
                request.retentionMs() != null ? request.retentionMs() : 604_800_000L));
        configs.put("cleanup.policy",
                request.cleanupPolicy() != null ? request.cleanupPolicy() : "delete");
        configs.put("min.insync.replicas", String.valueOf(
                request.minInsyncReplicas() != null ? request.minInsyncReplicas() : 1));

        NewTopic newTopic = new NewTopic(request.name(), request.partitions(), (short) request.replicationFactor())
                .configs(configs);
        try {
            adminClient.createTopics(List.of(newTopic)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Topico creado | name={} | partitions={} | replicationFactor={}",
                    request.name(), request.partitions(), request.replicationFactor());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Creacion de topico interrumpida");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                throw new BusinessException("El topico " + request.name() + " ya existe");
            }
            throw new BusinessException("No se pudo crear el topico: " + rootMessage(ex));
        } catch (TimeoutException ex) {
            throw new BusinessException("Timeout creando el topico " + request.name());
        }
    }

    @Override
    public void updateTopicConfig(String topic, UpdateTopicConfigRequest request) {
        readTopic(topic); // 404 si no existe

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        Collection<AlterConfigOp> operations = request.configs().entrySet().stream()
                .map(entry -> new AlterConfigOp(new ConfigEntry(entry.getKey(), entry.getValue()),
                        AlterConfigOp.OpType.SET))
                .toList();

        try {
            adminClient.incrementalAlterConfigs(Map.of(resource, operations))
                    .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Configuracion actualizada | topic={} | configs={}", topic, request.configs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Actualizacion de configuracion interrumpida");
        } catch (ExecutionException | TimeoutException ex) {
            throw new BusinessException("No se pudo actualizar la configuracion: " + rootMessage(ex));
        }
    }

    @Override
    public void deleteTopic(String topic) {
        readTopic(topic); // 404 si no existe
        try {
            adminClient.deleteTopics(List.of(topic)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Topico eliminado | name={}", topic);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Borrado de topico interrumpido");
        } catch (ExecutionException | TimeoutException ex) {
            throw new BusinessException("No se pudo eliminar el topico: " + rootMessage(ex));
        }
    }

    @Override
    public List<String> listConsumerGroups() {
        try {
            return adminClient.listConsumerGroups().valid().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .stream()
                    .map(group -> group.groupId())
                    .sorted()
                    .toList();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Consulta interrumpida al cluster de Kafka");
        } catch (ExecutionException | TimeoutException ex) {
            throw new BusinessException("No se pudieron listar los grupos: " + rootMessage(ex));
        }
    }

    @Override
    public ConsumerGroupDetail describeConsumerGroup(String groupId) {
        ConsumerGroupDescription description;
        try {
            description = adminClient.describeConsumerGroups(List.of(groupId))
                    .describedGroups().get(groupId).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Consulta interrumpida al cluster de Kafka");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof org.apache.kafka.common.errors.GroupIdNotFoundException) {
                throw new ResourceNotFoundException("Grupo no encontrado: " + groupId);
            }
            throw new BusinessException("No se pudo describir el grupo: " + rootMessage(ex));
        } catch (TimeoutException ex) {
            throw new BusinessException("Timeout describiendo el grupo " + groupId);
        }

        List<ConsumerGroupDetail.Member> members = description.members().stream()
                .map(this::toMember)
                .toList();

        return new ConsumerGroupDetail(groupId, description.state().toString(), members,
                partitionLags(groupId));
    }

    private TopicDetail readTopic(String topic) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        try {
            TopicDescription description = adminClient.describeTopics(List.of(topic))
                    .allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get(topic);
            Config config = adminClient.describeConfigs(List.of(resource))
                    .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get(resource);

            List<TopicDetail.PartitionInfo> partitions = new ArrayList<>();
            for (TopicPartitionInfo info : description.partitions()) {
                partitions.add(new TopicDetail.PartitionInfo(
                        info.partition(),
                        info.leader() != null ? info.leader().id() : -1,
                        info.replicas().stream().map(node -> node.id()).toList(),
                        info.isr().stream().map(node -> node.id()).toList()));
            }

            Map<String, String> configs = config.entries().stream()
                    .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value,
                            (a, b) -> a, java.util.LinkedHashMap::new));

            return new TopicDetail(description.name(), description.isInternal(), partitions, configs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Consulta interrumpida al cluster de Kafka");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new ResourceNotFoundException("Topico no encontrado: " + topic);
            }
            throw new BusinessException("No se pudo describir el topico: " + rootMessage(ex));
        } catch (TimeoutException ex) {
            throw new BusinessException("Timeout describiendo el topico " + topic);
        }
    }

    private List<ConsumerGroupDetail.PartitionLag> partitionLags(String groupId) {
        Map<TopicPartition, OffsetAndMetadata> committed;
        try {
            committed = adminClient.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (ExecutionException | TimeoutException ex) {
            log.warn("No se pudieron leer los offsets del grupo {}: {}", groupId, rootMessage(ex));
            return List.of();
        }
        if (committed == null || committed.isEmpty()) {
            return List.of();
        }

        Map<TopicPartition, OffsetSpec> latestRequest = committed.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<TopicPartition, org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo> latest;
        try {
            latest = adminClient.listOffsets(latestRequest).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (ExecutionException | TimeoutException ex) {
            log.warn("No se pudo leer el log-end del grupo {}: {}", groupId, rootMessage(ex));
            return List.of();
        }

        List<ConsumerGroupDetail.PartitionLag> result = new ArrayList<>();
        committed.forEach((tp, offset) -> {
            long current = offset != null ? offset.offset() : -1L;
            var end = latest.get(tp);
            long logEnd = end != null ? end.offset() : -1L;
            long lag = (current >= 0 && logEnd >= 0) ? Math.max(0, logEnd - current) : -1L;
            result.add(new ConsumerGroupDetail.PartitionLag(tp.topic(), tp.partition(), current, logEnd, lag));
        });
        result.sort(Comparator.comparing(ConsumerGroupDetail.PartitionLag::topic)
                .thenComparingInt(ConsumerGroupDetail.PartitionLag::partition));
        return result;
    }

    private ConsumerGroupDetail.Member toMember(MemberDescription member) {
        return new ConsumerGroupDetail.Member(
                member.consumerId(),
                member.clientId(),
                member.host());
    }

    private String rootMessage(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
