package com.example.charging.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeoutException;

@Component
public class KafkaTopicInitializer implements ApplicationRunner {

    private final KafkaAdmin kafkaAdmin;
    private final KafkaListenerEndpointRegistry listenerRegistry;
    private final RetryTemplate retryTemplate;

    @Autowired
    public KafkaTopicInitializer(
            KafkaAdmin kafkaAdmin,
            KafkaListenerEndpointRegistry listenerRegistry) {
        this(kafkaAdmin, listenerRegistry, defaultRetryTemplate());
    }

    KafkaTopicInitializer(
            KafkaAdmin kafkaAdmin,
            KafkaListenerEndpointRegistry listenerRegistry,
            RetryTemplate retryTemplate) {
        this.kafkaAdmin = kafkaAdmin;
        this.listenerRegistry = listenerRegistry;
        this.retryTemplate = retryTemplate;
    }

    private static RetryTemplate defaultRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(5)
                .exponentialBackoff(1_000, 2.0, 16_000)
                .retryOn(KafkaTransientException.class)
                .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        retryTemplate.execute(context -> {
            try {
                if (!kafkaAdmin.initialize()) {
                    throw new IllegalStateException("Kafka topic initialization failed");
                }
            } catch (Exception exception) {
                if (isTransient(exception)) {
                    throw new KafkaTransientException(exception); // 재시도 대상(일시적 오류)
                }
                throw exception; // 즉시 실패(영구 오류)
            }
            return null;
        });
        listenerRegistry.start();
    }

    private static boolean isTransient(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RetriableException || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class KafkaTransientException extends RuntimeException {

        private KafkaTransientException(Throwable cause) {
            super("Kafka is temporarily unavailable", cause);
        }
    }
}
