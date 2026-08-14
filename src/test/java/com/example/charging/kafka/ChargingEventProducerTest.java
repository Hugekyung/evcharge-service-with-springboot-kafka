package com.example.charging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.charging.application.ChargingEventPublishCommand;
import com.example.charging.application.ChargingEventPublishException;
import com.example.charging.domain.ChargingEventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.example.charging.application.ChargingEventPublisher;
import com.example.charging.controller.ChargingEventController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@ExtendWith(MockitoExtension.class)
class ChargingEventProducerTest {

    private static final ChargingEventPublishCommand COMMAND = new ChargingEventPublishCommand(
            "evt-100001",
            "charger-001",
            "session-001",
            ChargingEventType.CHARGING_STARTED,
            1,
            35,
            new BigDecimal("12.50"),
            Instant.parse("2026-08-12T03:00:00Z"));

    @Mock
    private KafkaTemplate<String, ChargingEventMessage> kafkaTemplate;

    @AfterEach
    void clearInterruptedFlag() {
        Thread.interrupted();
    }

    @Test
    void publishesMappedMessageToTheChargingEventsTopicWithTheSessionIdKey() throws Exception {
        // Given
        CompletableFuture<SendResult<String, ChargingEventMessage>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("charging-events"), eq(COMMAND.sessionId()), any(ChargingEventMessage.class)))
                .thenReturn(future);
        ChargingEventProducer producer = new ChargingEventProducer(kafkaTemplate);

        // When
        producer.publish(COMMAND);

        // Then
        ArgumentCaptor<ChargingEventMessage> message = ArgumentCaptor.forClass(ChargingEventMessage.class);
        verify(kafkaTemplate).send(eq("charging-events"), eq(COMMAND.sessionId()), message.capture());
        assertThat(message.getValue()).isEqualTo(new ChargingEventMessage(
                COMMAND.eventId(),
                COMMAND.chargerId(),
                COMMAND.sessionId(),
                COMMAND.eventType(),
                COMMAND.sequence(),
                COMMAND.batteryLevel(),
                COMMAND.chargedKwh(),
                COMMAND.occurredAt()));
    }

    @Test
    void translatesBrokerExecutionFailuresToPublishExceptions() throws Exception {
        // Given
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        ChargingEventProducer producer = new ChargingEventProducer(kafkaTemplate);

        // When / Then
        assertThatThrownBy(() -> producer.publish(COMMAND))
                .isInstanceOf(ChargingEventPublishException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void translatesBrokerTimeoutsToPublishExceptions() throws Exception {
        // Given
        TimeoutFuture<SendResult<String, ChargingEventMessage>> future = new TimeoutFuture<>();
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
        ChargingEventProducer producer = new ChargingEventProducer(kafkaTemplate);

        // When / Then
        assertThatThrownBy(() -> producer.publish(COMMAND))
                .isInstanceOf(ChargingEventPublishException.class)
                .hasCauseInstanceOf(TimeoutException.class);
        assertThat(future.timeout).isPositive().isLessThanOrEqualTo(TimeUnit.SECONDS.toNanos(3));
        assertThat(future.unit).isEqualTo(TimeUnit.NANOSECONDS);
    }

    @Test
    void keepsTheWholePublishAttemptWithinThreeSecondsWhenSendingBlocksBeforeReturningAFuture() throws Exception {
        // Given
        CountDownLatch sendReturnDelay = new CountDownLatch(1);
        CompletableFuture<SendResult<String, ChargingEventMessage>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(), any(), any())).thenAnswer(invocation -> {
            sendReturnDelay.await(1, TimeUnit.SECONDS);
            return future;
        });
        ChargingEventProducer producer = new ChargingEventProducer(kafkaTemplate);

        // When
        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> producer.publish(COMMAND))
                // Then
                .isInstanceOf(ChargingEventPublishException.class)
                .hasCauseInstanceOf(TimeoutException.class);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        // Then
        assertThat(elapsedMillis).isLessThan(3_500);
    }

    @Test
    void restoresTheInterruptedFlagWhenBrokerPublishIsInterrupted() throws Exception {
        // Given
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new InterruptedFuture<>());
        ChargingEventProducer producer = new ChargingEventProducer(kafkaTemplate);

        // When / Then
        assertThatThrownBy(() -> producer.publish(COMMAND))
                .isInstanceOf(ChargingEventPublishException.class)
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void wiresTheSoleProductionPublisherIntoTheControllerWhenKafkaTemplateIsSupplied() {
        // Given
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(ProductionPublisherWiringConfiguration.class);

        // When / Then
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChargingEventPublisher.class);
            assertThat(context).hasSingleBean(ChargingEventProducer.class);
            assertThat(context).hasSingleBean(ChargingEventController.class);
            assertThat(context.getBean(ChargingEventPublisher.class))
                    .isSameAs(context.getBean(ChargingEventProducer.class));
        });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackageClasses = {ChargingEventProducer.class, ChargingEventController.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {ChargingEventProducer.class, ChargingEventController.class}))
    static class ProductionPublisherWiringConfiguration {

        @Bean
        KafkaTemplate<String, ChargingEventMessage> kafkaTemplate() {
            return mock();
        }
    }

    private static final class TimeoutFuture<T> extends CompletableFuture<T> {

        private long timeout;
        private TimeUnit unit;

        @Override
        public T get(long timeout, TimeUnit unit) throws TimeoutException {
            this.timeout = timeout;
            this.unit = unit;
            throw new TimeoutException("broker timeout");
        }
    }

    private static final class InterruptedFuture<T> extends CompletableFuture<T> {

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("interrupted");
        }
    }
}
