package com.example.charging.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.charging.domain.ChargingEventType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChargingEventPublisherContractTest {

    @Test
    void publishReturnsNormallyWhenBrokerAcknowledges() {
        // Given
        ChargingEventPublisher publisher = command -> { };
        ChargingEventPublishCommand command = command();

        // When / Then
        assertThatCode(() -> publisher.publish(command)).doesNotThrowAnyException();
    }

    @Test
    void publishSignalsBrokerFailureWithPublishingException() {
        // Given
        ChargingEventPublisher publisher = command -> {
            throw new ChargingEventPublishException("broker publish failed");
        };

        // When / Then
        assertThatThrownBy(() -> publisher.publish(command()))
                .isInstanceOf(ChargingEventPublishException.class);
    }

    private ChargingEventPublishCommand command() {
        return new ChargingEventPublishCommand(
                "evt-100001",
                "charger-001",
                "session-001",
                ChargingEventType.CHARGING_STARTED,
                1,
                35,
                new BigDecimal("0"),
                Instant.parse("2026-08-12T03:00:00Z"));
    }
}
