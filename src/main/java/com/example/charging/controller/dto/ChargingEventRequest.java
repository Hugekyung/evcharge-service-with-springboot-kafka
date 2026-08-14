package com.example.charging.controller.dto;

import com.example.charging.domain.ChargingEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record ChargingEventRequest(
        @NotBlank @Pattern(regexp = "^[^\\p{Cntrl}]*$") String eventId,
        @NotBlank @Pattern(regexp = "^[^\\p{Cntrl}]*$") String chargerId,
        @NotBlank @Pattern(regexp = "^[^\\p{Cntrl}]*$") String sessionId,
        @NotNull ChargingEventType eventType,
        @Positive long sequence,
        Integer batteryLevel,
        BigDecimal chargedKwh,
        @NotNull Instant occurredAt) {
}
