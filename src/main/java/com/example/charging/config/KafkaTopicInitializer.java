package com.example.charging.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicInitializer implements ApplicationRunner {

    private final KafkaAdmin kafkaAdmin;
    private final KafkaListenerEndpointRegistry listenerRegistry;
    private final RetryTemplate retryTemplate;

    public KafkaTopicInitializer(
            KafkaAdmin kafkaAdmin,
            KafkaListenerEndpointRegistry listenerRegistry) {
        this.kafkaAdmin = kafkaAdmin;
        this.listenerRegistry = listenerRegistry;
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(5)
                .exponentialBackoff(1_000, 2.0, 16_000)
                .retryOn(Exception.class)
                .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        // Kafka 연결·토픽 생성 재시도
        retryTemplate.execute(context -> {
            if (!kafkaAdmin.initialize()) {
                throw new IllegalStateException("Kafka topic initialization failed");
            }
            return null;
        });
        listenerRegistry.start(); // 성공한 뒤 Consumer 시작
    }
}
