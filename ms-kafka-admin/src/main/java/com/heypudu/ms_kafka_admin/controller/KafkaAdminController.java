package com.heypudu.ms_kafka_admin.controller;

import com.heypudu.ms_kafka_admin.dto.ClusterInfo;
import com.heypudu.ms_kafka_admin.dto.ConsumerGroupDetail;
import com.heypudu.ms_kafka_admin.dto.CreateTopicRequest;
import com.heypudu.ms_kafka_admin.dto.TopicDetail;
import com.heypudu.ms_kafka_admin.dto.TopicSummary;
import com.heypudu.ms_kafka_admin.dto.UpdateTopicConfigRequest;
import com.heypudu.ms_kafka_admin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administración de Kafka: tópicos, configuraciones y grupos de consumo.
 *
 * <p>Todas las operaciones exigen {@code ROLE_ADMIN}, igual que el
 * administrador de RabbitMQ.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/kafka")
@PreAuthorize("hasRole('ADMIN')")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @GetMapping("/cluster")
    public ResponseEntity<ClusterInfo> describeCluster() {
        return ResponseEntity.ok(kafkaAdminService.describeCluster());
    }

    @GetMapping("/topics")
    public ResponseEntity<List<TopicSummary>> listTopics() {
        return ResponseEntity.ok(kafkaAdminService.listTopics());
    }

    @GetMapping("/topics/{topic}")
    public ResponseEntity<TopicDetail> describeTopic(@PathVariable String topic) {
        return ResponseEntity.ok(kafkaAdminService.describeTopic(topic));
    }

    @PostMapping("/topics")
    public ResponseEntity<Void> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        kafkaAdminService.createTopic(request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/topics/{topic}/config")
    public ResponseEntity<Void> updateTopicConfig(
            @PathVariable String topic,
            @Valid @RequestBody UpdateTopicConfigRequest request) {
        kafkaAdminService.updateTopicConfig(topic, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/topics/{topic}")
    public ResponseEntity<Void> deleteTopic(@PathVariable String topic) {
        kafkaAdminService.deleteTopic(topic);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups")
    public ResponseEntity<List<String>> listGroups() {
        return ResponseEntity.ok(kafkaAdminService.listConsumerGroups());
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ConsumerGroupDetail> describeGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(kafkaAdminService.describeConsumerGroup(groupId));
    }
}
