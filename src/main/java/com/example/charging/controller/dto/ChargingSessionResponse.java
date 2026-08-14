package com.example.charging.controller.dto;

import com.example.charging.domain.ChargingSession;
import com.example.charging.domain.ChargingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ChargingSessionResponse(
        String sessionId,
        String chargerId,
        ChargingSessionStatus status,
        Integer batteryLevel,
        BigDecimal chargedKwh,
        long lastSequence,
        Instant startedAt,
        Instant completedAt) {

    public static ChargingSessionResponse from(ChargingSession session) {
        return new ChargingSessionResponse(
                session.getSessionId(),
                session.getChargerId(),
                session.getStatus(),
                session.getBatteryLevel(),
                session.getChargedKwh(),
                session.getLastSequence(),
                session.getStartedAt(),
                session.getCompletedAt());
    }
}
