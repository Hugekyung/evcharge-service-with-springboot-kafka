package com.example.charging.controller.dto;

import java.util.List;

public record ChargingEventHistoryResponse(
        List<ChargingEventResponse> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext) {
}
