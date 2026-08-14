package com.example.charging.application;

import com.example.charging.domain.ChargingEvent;
import com.example.charging.domain.ChargingSession;
import com.example.charging.kafka.ChargingEventMessage;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChargingSessionService {

    void process(ChargingEventMessage message);

    ChargingSession getBySessionId(String sessionId);

    Page<ChargingEvent> getEventsBySessionId(String sessionId, Pageable pageable);
}
