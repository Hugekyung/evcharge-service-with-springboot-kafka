package com.example.charging.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.charging.domain.ChargingEventType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChargingEventPublisherContractTest {

    @Test
    @DisplayName("브로커가 승인하면 이벤트 발행이 정상 종료된다")
    void publishReturnsNormallyWhenBrokerAcknowledges() {
        // Given
        ChargingEventPublisher publisher = command -> { };
        ChargingEventPublishCommand command = command();

        // When / Then
        assertThatCode(() -> publisher.publish(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("브로커 발행 실패를 발행 예외로 전달한다")
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
