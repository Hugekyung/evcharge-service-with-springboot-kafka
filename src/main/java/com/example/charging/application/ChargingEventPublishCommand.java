package com.example.charging.application;

import com.example.charging.domain.ChargingEventType;
import java.math.BigDecimal;
import java.time.Instant;

public record ChargingEventPublishCommand(
        String eventId,
        String chargerId,
        String sessionId,
        ChargingEventType eventType,
        long sequence,
        Integer batteryLevel,
        BigDecimal chargedKwh,
        Instant occurredAt) {
}
