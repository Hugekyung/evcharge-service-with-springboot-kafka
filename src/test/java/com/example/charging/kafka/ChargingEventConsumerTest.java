package com.example.charging.kafka;

import static org.mockito.Mockito.verify;

import com.example.charging.application.ChargingSessionService;
import com.example.charging.domain.ChargingEventType;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.RetryableTopic;
import static org.assertj.core.api.Assertions.assertThat;

class ChargingEventConsumerTest {

    private static ChargingEventMessage message() {
        return new ChargingEventMessage(
                "evt-100001",
                "charger-001",
                "session-001",
                ChargingEventType.CHARGING_STARTED,
                1,
                35,
                new BigDecimal("12.50"),
                Instant.parse("2026-08-12T03:00:00Z"));
    }

    @Test
    void delegatesReceivedMessageToChargingSessionService() {
        ChargingSessionService service = org.mockito.Mockito.mock(ChargingSessionService.class);
        ChargingEventConsumer consumer = new ChargingEventConsumer(service);
        ChargingEventMessage message = message();

        consumer.consume(message);

        verify(service).process(message);
    }

    @Test
    void declaresThreeAttemptsAndExponentialBackoff() throws NoSuchMethodException {
        Method consume = ChargingEventConsumer.class.getMethod("consume", ChargingEventMessage.class);
        RetryableTopic retryableTopic = consume.getAnnotation(RetryableTopic.class);

        assertThat(retryableTopic).isNotNull();
        assertThat(retryableTopic.attempts()).isEqualTo("3");
        assertThat(retryableTopic.backoff().delay()).isEqualTo(1_000L);
        assertThat(retryableTopic.backoff().multiplier()).isEqualTo(2.0);
        assertThat(retryableTopic.backoff().maxDelay()).isEqualTo(16_000L);
        assertThat(retryableTopic.include())
                .containsExactly(TransientDataAccessException.class, DataAccessResourceFailureException.class, RetriableException.class)
                .doesNotContain(com.example.charging.application.ChargingEventBusinessException.class);
    }

    @Test
    void acceptsFailedEventAtDltHandler() {
        ChargingEventConsumer consumer = new ChargingEventConsumer(org.mockito.Mockito.mock(ChargingSessionService.class));

        consumer.handleDlt(message());
    }
}
