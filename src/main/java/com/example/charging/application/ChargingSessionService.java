package com.example.charging.application;

import com.example.charging.domain.ChargingEvent;
import com.example.charging.domain.ChargingSession;
import com.example.charging.kafka.ChargingEventMessage;
import java.util.List;

public interface ChargingSessionService {

    void process(ChargingEventMessage message);

    ChargingSession getBySessionId(String sessionId);

    List<ChargingEvent> getEventsBySessionId(String sessionId);
}
