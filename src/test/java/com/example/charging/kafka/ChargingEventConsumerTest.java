package com.example.charging.kafka;

import static org.mockito.Mockito.verify;

import com.example.charging.application.ChargingSessionService;
import com.example.charging.domain.ChargingEventType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChargingEventConsumerTest {

    @Test
    void delegatesReceivedMessageToChargingSessionService() {
        ChargingSessionService service = org.mockito.Mockito.mock(ChargingSessionService.class);
        ChargingEventConsumer consumer = new ChargingEventConsumer(service);
        ChargingEventMessage message = new ChargingEventMessage(
                "evt-100001",
                "charger-001",
                "session-001",
                ChargingEventType.CHARGING_STARTED,
                1,
                35,
                new BigDecimal("12.50"),
                Instant.parse("2026-08-12T03:00:00Z"));

        consumer.consume(message);

        verify(service).process(message);
    }
}
