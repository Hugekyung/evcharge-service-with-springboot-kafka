package com.example.charging.kafka;

import com.example.charging.application.ChargingSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ChargingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChargingEventConsumer.class);

    private final ChargingSessionService chargingSessionService;

    public ChargingEventConsumer(ChargingSessionService chargingSessionService) {
        this.chargingSessionService = chargingSessionService;
    }

    @KafkaListener(topics = "charging-events")
    public void consume(ChargingEventMessage message) {
        chargingSessionService.process(message);
    }

    @KafkaListener(
            topics = "charging-events-dlt",
            groupId = "evcharging-dlt",
            containerFactory = "dltKafkaListenerContainerFactory")
    public void handleDlt(ChargingEventMessage message) {
        log.error(
                "Charging event moved to DLT: eventId={}, sessionId={}, chargerId={}, eventType={}, sequence={}",
                message.eventId(),
                message.sessionId(),
                message.chargerId(),
                message.eventType(),
                message.sequence());
    }
}
