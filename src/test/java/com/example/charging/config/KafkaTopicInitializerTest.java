package com.example.charging.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.retry.support.RetryTemplate;

class KafkaTopicInitializerTest {

    @Test
    @DisplayName("Kafka 연결이 일시적으로 실패해도 재시도 성공 후 Listener를 시작한다")
    void retriesKafkaInitializationThenStartsListeners() {
        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        KafkaListenerEndpointRegistry listenerRegistry = mock(KafkaListenerEndpointRegistry.class);
        when(kafkaAdmin.initialize())
                .thenThrow(new TimeoutException("broker unavailable"))
                .thenReturn(true);
        KafkaTopicInitializer initializer = new KafkaTopicInitializer(
                kafkaAdmin,
                listenerRegistry,
                zeroDelayRetryTemplate());

        initializer.run(mock(ApplicationArguments.class));

        verify(kafkaAdmin, times(2)).initialize();
        verify(listenerRegistry).start();
    }

    @Test
    @DisplayName("Kafka 연결이 계속 실패하면 최대 재시도 후 Listener를 시작하지 않는다")
    void stopsAfterMaximumKafkaInitializationAttempts() {
        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        KafkaListenerEndpointRegistry listenerRegistry = mock(KafkaListenerEndpointRegistry.class);
        when(kafkaAdmin.initialize()).thenThrow(new TimeoutException("broker unavailable"));
        KafkaTopicInitializer initializer = new KafkaTopicInitializer(
                kafkaAdmin,
                listenerRegistry,
                zeroDelayRetryTemplate());

        assertThatThrownBy(() -> initializer.run(mock(ApplicationArguments.class)))
                .isInstanceOf(RuntimeException.class);

        verify(kafkaAdmin, times(5)).initialize();
        verify(listenerRegistry, never()).start();
    }

    private static RetryTemplate zeroDelayRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(5)
                .fixedBackoff(1)
                .retryOn(RuntimeException.class)
                .build();
    }
}
