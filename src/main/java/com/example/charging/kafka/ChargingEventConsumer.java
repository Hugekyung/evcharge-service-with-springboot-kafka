package com.example.charging.kafka;

import com.example.charging.application.ChargingSessionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ChargingEventConsumer {

    private final ChargingSessionService chargingSessionService;

    public ChargingEventConsumer(ChargingSessionService chargingSessionService) {
        this.chargingSessionService = chargingSessionService;
    }

    @KafkaListener(topics = "charging-events")
    public void consume(ChargingEventMessage message) {
        chargingSessionService.process(message);
    }
}
