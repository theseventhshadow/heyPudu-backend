package com.heypudu.ms_kafka_admin.service;

import com.heypudu.ms_kafka_admin.dto.ClusterInfo;
import com.heypudu.ms_kafka_admin.dto.ConsumerGroupDetail;
import com.heypudu.ms_kafka_admin.dto.CreateTopicRequest;
import com.heypudu.ms_kafka_admin.dto.TopicDetail;
import com.heypudu.ms_kafka_admin.dto.TopicSummary;
import com.heypudu.ms_kafka_admin.dto.UpdateTopicConfigRequest;

import java.util.List;

/**
 * Operaciones de administración sobre Kafka.
 */
public interface KafkaAdminService {

    ClusterInfo describeCluster();

    List<TopicSummary> listTopics();

    TopicDetail describeTopic(String topic);

    void createTopic(CreateTopicRequest request);

    void updateTopicConfig(String topic, UpdateTopicConfigRequest request);

    void deleteTopic(String topic);

    List<String> listConsumerGroups();

    ConsumerGroupDetail describeConsumerGroup(String groupId);
}
