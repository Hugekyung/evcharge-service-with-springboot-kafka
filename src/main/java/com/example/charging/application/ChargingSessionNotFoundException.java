package com.example.charging.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ChargingSessionNotFoundException extends RuntimeException {

    public ChargingSessionNotFoundException(String sessionId) {
        super("Charging session not found: " + sessionId);
    }
}
