package com.example.charging.kafka;

import com.example.charging.domain.ChargingEventType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record ChargingEventMessage(
        String eventId,
        String chargerId,
        String sessionId,
        ChargingEventType eventType,
        long sequence,
        Integer batteryLevel,
        BigDecimal chargedKwh,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant occurredAt) {
}
