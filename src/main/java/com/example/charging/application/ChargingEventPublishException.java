package com.example.charging.application;

public class ChargingEventPublishException extends RuntimeException {

    public ChargingEventPublishException(String message) {
        super(message);
    }

    public ChargingEventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
