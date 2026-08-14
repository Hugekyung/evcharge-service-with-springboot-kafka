package com.example.charging.controller;

import com.example.charging.application.ChargingEventPublishCommand;
import com.example.charging.application.ChargingEventPublishException;
import com.example.charging.application.ChargingEventPublisher;
import com.example.charging.controller.dto.ChargingEventRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/charging-events")
public class ChargingEventController {

    private static final Logger log = LoggerFactory.getLogger(ChargingEventController.class);

    private final ChargingEventPublisher publisher;

    public ChargingEventController(ChargingEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<Void> publish(@Valid @RequestBody ChargingEventRequest request) {
        ChargingEventPublishCommand command = new ChargingEventPublishCommand(
                request.eventId(),
                request.chargerId(),
                request.sessionId(),
                request.eventType(),
                request.sequence(),
                request.batteryLevel(),
                request.chargedKwh(),
                request.occurredAt());

        try {
            publisher.publish(command);
            return ResponseEntity.accepted().build();
        } catch (ChargingEventPublishException exception) {
            log.error(
                    "Charging event publish failed: eventId={}, sessionId={}, chargerId={}, eventType={}, sequence={}",
                    command.eventId(),
                    command.sessionId(),
                    command.chargerId(),
                    command.eventType(),
                    command.sequence(),
                    exception);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
