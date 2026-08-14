package com.example.charging.controller.dto;

import com.example.charging.domain.ChargingEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record ChargingEventRequest(
        @NotBlank String eventId,
        @NotBlank String chargerId,
        @NotBlank String sessionId,
        @NotNull ChargingEventType eventType,
        @Positive long sequence,
        Integer batteryLevel,
        BigDecimal chargedKwh,
        @NotNull Instant occurredAt) {
}
