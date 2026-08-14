package com.example.charging.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.charging.application.ChargingEventBusinessException;
import com.example.charging.application.ChargingSessionService;
import com.example.charging.application.ChargingSessionNotFoundException;
import com.example.charging.domain.ChargingEvent;
import com.example.charging.domain.ChargingEventType;
import com.example.charging.domain.ChargingSession;
import com.example.charging.domain.ChargingSessionStatus;
import com.example.charging.kafka.ChargingEventMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    @Test
    void returnsEventHistoryForExistingSession() throws Exception {
        service.session = ChargingSession.start(
                "session-1", "charger-1", 1, 55, new BigDecimal("12.500"),
                Instant.parse("2026-08-12T03:00:00Z"),
                Instant.parse("2026-08-12T03:00:01Z"));
        service.events = List.of(ChargingEvent.create(
                "evt-1", "session-1", "charger-1", ChargingEventType.CHARGING_STARTED,
                1, 55, new BigDecimal("12.500"),
                Instant.parse("2026-08-12T03:00:00Z"),
                Instant.parse("2026-08-12T03:00:01Z")));

        mockMvc.perform(get("/api/v1/charging-sessions/session-1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$.content[0].sequence").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value("CHARGING_STARTED"));
    }

    @Test
    void rejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/charging-sessions/session-1/events").param("size", "101"))
                .andExpect(status().isBadRequest());
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
        private List<ChargingEvent> events = List.of();
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

        @Override
        public Page<ChargingEvent> getEventsBySessionId(String sessionId, Pageable pageable) {
            getBySessionId(sessionId);
            return new PageImpl<>(events, pageable, events.size());
        }

        void reset() {
            session = null;
            events = List.of();
            notFound = false;
        }
    }
}
