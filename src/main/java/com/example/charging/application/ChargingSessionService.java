package com.example.charging.application;

import com.example.charging.kafka.ChargingEventMessage;
import com.example.charging.domain.ChargingSession;

public interface ChargingSessionService {

    void process(ChargingEventMessage message);

    ChargingSession getBySessionId(String sessionId);
}
