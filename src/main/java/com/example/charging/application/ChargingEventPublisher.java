package com.example.charging.application;

public interface ChargingEventPublisher {

    /**
     * Publishes only after the broker acknowledges the command, or throws when publishing fails or times out.
     */
    void publish(ChargingEventPublishCommand command);
}
