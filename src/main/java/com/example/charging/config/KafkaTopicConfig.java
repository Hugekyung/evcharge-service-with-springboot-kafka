package com.example.charging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic chargingEventsTopic() {
        return TopicBuilder.name("charging-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
