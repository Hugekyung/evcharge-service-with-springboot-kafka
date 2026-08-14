package com.example.charging.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.charging.application.ChargingEventBusinessException;
import com.example.charging.application.ChargingSessionService;
import com.example.charging.application.ChargingSessionNotFoundException;
import com.example.charging.domain.ChargingSession;
import com.example.charging.domain.ChargingSessionStatus;
import com.example.charging.kafka.ChargingEventMessage;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChargingSessionController.class)
@Import(ChargingSessionControllerTest.SessionServiceTestConfiguration.class)
class ChargingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingSessionService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    void returnsSessionResponseWhenSessionExists() throws Exception {
        service.session = ChargingSession.start(
                "session-1",
                "charger-1",
                3,
                55,
                new BigDecimal("12.500"),
                Instant.parse("2026-08-12T03:00:00Z"),
                Instant.parse("2026-08-12T03:00:01Z"));

        mockMvc.perform(get("/api/v1/charging-sessions/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.chargerId").value("charger-1"))
                .andExpect(jsonPath("$.status").value(ChargingSessionStatus.CHARGING.name()))
                .andExpect(jsonPath("$.lastSequence").value(3));
    }

    @Test
    void returnsNotFoundWhenSessionDoesNotExist() throws Exception {
        service.notFound = true;

        mockMvc.perform(get("/api/v1/charging-sessions/unknown"))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SessionServiceTestConfiguration {

        @Bean
        RecordingSessionService recordingSessionService() {
            return new RecordingSessionService();
        }
    }

    static class RecordingSessionService implements ChargingSessionService {

        private ChargingSession session;
        private boolean notFound;

        @Override
        public void process(ChargingEventMessage message) {
            throw new ChargingEventBusinessException("not used");
        }

        @Override
        public ChargingSession getBySessionId(String sessionId) {
            if (notFound) {
                throw new ChargingSessionNotFoundException(sessionId);
            }
            return session;
        }

        void reset() {
            session = null;
            notFound = false;
        }
    }
}
