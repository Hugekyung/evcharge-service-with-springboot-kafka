package com.example.charging.controller;

import com.example.charging.application.ChargingSessionService;
import com.example.charging.controller.dto.ChargingEventResponse;
import com.example.charging.controller.dto.ChargingEventHistoryResponse;
import com.example.charging.controller.dto.ChargingSessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/charging-sessions")
public class ChargingSessionController {

    private final ChargingSessionService chargingSessionService;

    public ChargingSessionController(ChargingSessionService chargingSessionService) {
        this.chargingSessionService = chargingSessionService;
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ChargingSessionResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(ChargingSessionResponse.from(
                chargingSessionService.getBySessionId(sessionId)));
    }

    @GetMapping("/{sessionId}/events")
    public ResponseEntity<ChargingEventHistoryResponse> getEvents(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }
        Page<ChargingEventResponse> events = chargingSessionService.getEventsBySessionId(
                        sessionId,
                        PageRequest.of(page, size, Sort.by("sequence").ascending()))
                .map(ChargingEventResponse::from);
        return ResponseEntity.ok(new ChargingEventHistoryResponse(
                events.getContent(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.hasNext()));
    }
}
