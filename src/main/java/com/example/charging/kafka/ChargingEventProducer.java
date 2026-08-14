package com.example.charging.kafka;

import com.example.charging.application.ChargingEventPublishCommand;
import com.example.charging.application.ChargingEventPublishException;
import com.example.charging.application.ChargingEventPublisher;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChargingEventProducer implements ChargingEventPublisher {

    private static final String TOPIC = "charging-events";
    private static final long PUBLISH_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(3);

    private final KafkaTemplate<String, ChargingEventMessage> kafkaTemplate;

    public ChargingEventProducer(KafkaTemplate<String, ChargingEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(ChargingEventPublishCommand command) {
        long deadline = System.nanoTime() + PUBLISH_TIMEOUT_NANOS;
        try {
            kafkaTemplate.send(TOPIC, command.sessionId(), toMessage(command))
                    .get(remainingNanos(deadline), TimeUnit.NANOSECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ChargingEventPublishException("Kafka event publishing was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new ChargingEventPublishException("Kafka event publishing failed", exception.getCause());
        } catch (TimeoutException exception) {
            throw new ChargingEventPublishException("Kafka event publishing timed out", exception);
        } catch (RuntimeException exception) {
            throw new ChargingEventPublishException("Kafka event publishing failed", exception);
        }
    }

    private long remainingNanos(long deadline) throws TimeoutException {
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
            throw new TimeoutException("Kafka event publishing timed out");
        }
        return remainingNanos;
    }

    private ChargingEventMessage toMessage(ChargingEventPublishCommand command) {
        return new ChargingEventMessage(
                command.eventId(),
                command.chargerId(),
                command.sessionId(),
                command.eventType(),
                command.sequence(),
                command.batteryLevel(),
                command.chargedKwh(),
                command.occurredAt());
    }
}
