package com.example.charging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
        var adminProperties = kafkaProperties.buildAdminProperties(null);
        adminProperties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3_000);
        adminProperties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3_000);
        adminProperties.put(AdminClientConfig.RETRIES_CONFIG, 0);
        KafkaAdmin kafkaAdmin = new KafkaAdmin(adminProperties);
        kafkaAdmin.setAutoCreate(false);
        kafkaAdmin.setFatalIfBrokerNotAvailable(true);
        kafkaAdmin.setOperationTimeout(3);
        return kafkaAdmin;
    }

    @Bean
    public NewTopic chargingEventsTopic() {
        return TopicBuilder.name("charging-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chargingEventsDltTopic() {
        return TopicBuilder.name("charging-events-dlt")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
