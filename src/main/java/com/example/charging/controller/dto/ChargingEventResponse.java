package com.example.charging.controller.dto;

import com.example.charging.domain.ChargingEvent;
import com.example.charging.domain.ChargingEventType;
import java.math.BigDecimal;
import java.time.Instant;

public record ChargingEventResponse(
        String eventId,
        String sessionId,
        String chargerId,
        ChargingEventType eventType,
        long sequence,
        Integer batteryLevel,
        BigDecimal chargedKwh,
        Instant occurredAt,
        Instant processedAt) {

    public static ChargingEventResponse from(ChargingEvent event) {
        return new ChargingEventResponse(
                event.getEventId(),
                event.getSessionId(),
                event.getChargerId(),
                event.getEventType(),
                event.getSequence(),
                event.getBatteryLevel(),
                event.getChargedKwh(),
                event.getOccurredAt(),
                event.getProcessedAt());
    }
}
