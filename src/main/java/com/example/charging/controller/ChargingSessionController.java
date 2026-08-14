package com.example.charging.controller;

import com.example.charging.application.ChargingSessionService;
import com.example.charging.controller.dto.ChargingEventResponse;
import com.example.charging.controller.dto.ChargingSessionResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<List<ChargingEventResponse>> getEvents(@PathVariable String sessionId) {
        List<ChargingEventResponse> events = chargingSessionService.getEventsBySessionId(sessionId)
                .stream()
                .map(ChargingEventResponse::from)
                .toList();
        return ResponseEntity.ok(events);
    }
}
