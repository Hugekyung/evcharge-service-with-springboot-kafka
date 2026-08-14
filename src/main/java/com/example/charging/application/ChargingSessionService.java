package com.example.charging.application;

import com.example.charging.kafka.ChargingEventMessage;

public interface ChargingSessionService {

    void process(ChargingEventMessage message);
}
