package com.example.charging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.charging.application.ChargingEventBusinessException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.BackOffHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.dao.TransientDataAccessException;

class KafkaConsumerConfigTest {

    @Test
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
