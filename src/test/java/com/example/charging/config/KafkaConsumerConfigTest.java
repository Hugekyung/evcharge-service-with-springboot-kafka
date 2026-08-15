package com.example.charging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.charging.application.ChargingEventBusinessException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.BackOffHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.dao.TransientDataAccessException;

class KafkaConsumerConfigTest {

    @Test
    @DisplayName("일시적 오류는 두 번 재시도한 뒤 DLT로 복구한다")
    void retriesTransientFailureTwiceThenRecoversToDlt() {
        AtomicInteger recoveries = new AtomicInteger();
        DefaultErrorHandler handler = newHandler(recoveries);
        ConsumerRecord<String, String> record = record();
        Consumer<String, String> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        RuntimeException failure = new TransientDataAccessException("temporary") {};

        assertThat(handler.handleOne(failure, record, consumer, container)).isFalse();
        assertThat(handler.handleOne(failure, record, consumer, container)).isFalse();
        assertThat(handler.handleOne(failure, record, consumer, container)).isTrue();
        assertThat(recoveries).hasValue(1);
    }

    @Test
    @DisplayName("비즈니스 오류는 재시도하지 않고 즉시 DLT로 복구한다")
    void recoversBusinessFailureWithoutRetry() {
        AtomicInteger recoveries = new AtomicInteger();
        DefaultErrorHandler handler = newHandler(recoveries);
        ConsumerRecord<String, String> record = record();
        Consumer<String, String> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        assertThat(handler.handleOne(
                new ChargingEventBusinessException("invalid transition"),
                record,
                consumer,
                container)).isTrue();
        assertThat(recoveries).hasValue(1);
    }

    @Test
    @DisplayName("예상하지 못한 RuntimeException은 재시도하지 않고 복구한다")
    void recoversUnexpectedRuntimeFailureWithoutRetry() {
        AtomicInteger recoveries = new AtomicInteger();
        DefaultErrorHandler handler = newHandler(recoveries);
        ConsumerRecord<String, String> record = record();
        Consumer<String, String> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        assertThat(handler.handleOne(new IllegalStateException("unexpected"), record, consumer, container))
                .isTrue();
        assertThat(recoveries).hasValue(1);
    }

    @Test
    @DisplayName("DLT Handler 오류는 다시 재시도하지 않는다")
    void doesNotRetryDltHandlerFailure() {
        AtomicInteger recoveries = new AtomicInteger();
        ConsumerRecordRecoverer recoverer = (record, exception) -> recoveries.incrementAndGet();
        DefaultErrorHandler handler = KafkaConsumerConfig.createDltErrorHandler(recoverer);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "charging-events-dlt", 0, 0L, "session-001", "event");

        assertThat(handler.handleOne(
                new IllegalStateException("dlt handler failed"),
                record,
                mock(Consumer.class),
                mock(MessageListenerContainer.class))).isTrue();
        assertThat(recoveries).hasValue(1);
    }

    private static DefaultErrorHandler newHandler(AtomicInteger recoveries) {
        BackOffHandler noWait = new BackOffHandler() { };
        ConsumerRecordRecoverer recoverer = (record, exception) -> recoveries.incrementAndGet();
        return KafkaConsumerConfig.createBlockingRetryErrorHandlerWithBackOffHandler(recoverer, noWait);
    }

    private static ConsumerRecord<String, String> record() {
        return new ConsumerRecord<>("charging-events", 0, 0L, "session-001", "event");
    }
}
