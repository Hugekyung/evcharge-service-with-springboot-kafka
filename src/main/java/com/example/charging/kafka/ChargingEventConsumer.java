package com.example.charging.kafka;

import com.example.charging.application.ChargingSessionService;
import org.apache.kafka.common.errors.RetriableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class ChargingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChargingEventConsumer.class);

    private final ChargingSessionService chargingSessionService;

    public ChargingEventConsumer(ChargingSessionService chargingSessionService) {
        this.chargingSessionService = chargingSessionService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 16_000),
            include = {TransientDataAccessException.class, DataAccessResourceFailureException.class, RetriableException.class},
            autoCreateTopics = "true",
            numPartitions = "3",
            replicationFactor = "1")
    @KafkaListener(topics = "charging-events")
    public void consume(ChargingEventMessage message) {
        chargingSessionService.process(message);
    }

    @DltHandler
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
