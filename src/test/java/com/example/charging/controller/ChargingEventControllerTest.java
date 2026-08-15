package com.example.charging.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.charging.application.ChargingEventPublishCommand;
import com.example.charging.application.ChargingEventPublishException;
import com.example.charging.application.ChargingEventPublisher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChargingEventController.class)
@Import(ChargingEventControllerTest.PublisherTestConfiguration.class)
class ChargingEventControllerTest {

    private static final String VALID_REQUEST = """
            {
              "eventId": "evt-100001",
              "chargerId": "charger-001",
              "sessionId": "session-001",
              "eventType": "CHARGING_STARTED",
              "sequence": 1,
              "batteryLevel": 35,
              "chargedKwh": 0,
              "occurredAt": "2026-08-12T12:00:00+09:00"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingPublisher publisher;

    @BeforeEach
    void resetPublisher() {
        publisher.reset();
    }

    @Test
    @DisplayName("브로커 승인 시 이벤트 API가 202 Accepted를 반환한다")
    void postReturnsAcceptedWhenPublisherAcknowledges() throws Exception {
        // Given
        publisher.acknowledge();

        // When
        mockMvc.perform(post("/api/v1/charging-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                // Then
                .andExpect(status().isAccepted());

        assertThat(publisher.commands()).hasSize(1);
        assertThat(publisher.commands().getFirst().occurredAt())
                .isEqualTo(Instant.parse("2026-08-12T03:00:00Z"));
    }

    @Test
    @DisplayName("브로커 발행 실패 시 이벤트 API가 5xx를 반환한다")
    void postReturns5xxWhenPublisherFails() throws Exception {
        // Given
        publisher.fail();

        // When
        mockMvc.perform(post("/api/v1/charging-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                // Then
                .andExpect(status().is5xxServerError());

        assertThat(publisher.commands()).hasSize(1);
    }

    @ParameterizedTest
    @DisplayName("필수 입력값이 유효하지 않으면 발행하지 않고 400을 반환한다")
    @MethodSource("invalidBoundaryRequests")
    void postReturnsBadRequestWithoutPublishingWhenRequiredFieldIsInvalid(String invalidRequest) throws Exception {
        // Given

        // When
        mockMvc.perform(post("/api/v1/charging-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                // Then
                .andExpect(status().isBadRequest());

        assertThat(publisher.commands()).isEmpty();
    }

    private static Stream<String> invalidBoundaryRequests() {
        return Stream.of(
                VALID_REQUEST.replace("\"eventId\": \"evt-100001\"", "\"eventId\": \"  \""),
                VALID_REQUEST.replace("\"chargerId\": \"charger-001\"", "\"chargerId\": \"  \""),
                VALID_REQUEST.replace("\"sessionId\": \"session-001\"", "\"sessionId\": \"  \""),
                VALID_REQUEST.replace("  \"eventType\": \"CHARGING_STARTED\",\n", ""),
                VALID_REQUEST.replace("CHARGING_STARTED", ""),
                VALID_REQUEST.replace("\"sequence\": 1", "\"sequence\": 0"),
                VALID_REQUEST.replace("\"sequence\": 1", "\"sequence\": -1"),
                VALID_REQUEST.replace(",\n  \"occurredAt\": \"2026-08-12T12:00:00+09:00\"", ""));
    }

    @Test
    @DisplayName("알 수 없는 이벤트 타입이면 발행하지 않고 400을 반환한다")
    void postReturnsBadRequestWithoutPublishingWhenEventTypeIsUnknown() throws Exception {
        // Given
        String invalidRequest = VALID_REQUEST.replace("CHARGING_STARTED", "UNKNOWN");

        // When
        mockMvc.perform(post("/api/v1/charging-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                // Then
                .andExpect(status().isBadRequest());

        assertThat(publisher.commands()).isEmpty();
    }

    @Test
    @DisplayName("시간대 오프셋이 없는 timestamp면 발행하지 않고 400을 반환한다")
    void postReturnsBadRequestWithoutPublishingWhenTimestampHasNoOffset() throws Exception {
        // Given
        String invalidRequest = VALID_REQUEST.replace("2026-08-12T12:00:00+09:00", "2026-08-12T12:00:00");

        // When
        mockMvc.perform(post("/api/v1/charging-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                // Then
                .andExpect(status().isBadRequest());

        assertThat(publisher.commands()).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("식별자에 제어 문자가 포함되면 발행하지 않고 400을 반환한다")
    @MethodSource("controlCharacterRequests")
    void postReturnsBadRequestWithoutPublishingWhenIdentifierContainsControlCharacter(String invalidRequest)
            throws Exception {
        mockMvc.perform(post("/api/v1/charging-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        assertThat(publisher.commands()).isEmpty();
    }

    private static Stream<String> controlCharacterRequests() {
        return Stream.of(
                VALID_REQUEST.replace("evt-100001", "evt-100001\\nforged"),
                VALID_REQUEST.replace("charger-001", "charger-001\\tforged"),
                VALID_REQUEST.replace("session-001", "session-001\\rforged"));
    }

    @Test
    @DisplayName("JSON 형식이 잘못되면 발행하지 않고 400을 반환한다")
    void postReturnsBadRequestWithoutPublishingWhenJsonIsMalformed() throws Exception {
        // Given
        String invalidRequest = "{\"eventId\":";

        // When
        mockMvc.perform(post("/api/v1/charging-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                // Then
                .andExpect(status().isBadRequest());

        assertThat(publisher.commands()).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PublisherTestConfiguration {

        @Bean
        RecordingPublisher recordingPublisher() {
            return new RecordingPublisher();
        }
    }

    static final class RecordingPublisher implements ChargingEventPublisher {

        private final List<ChargingEventPublishCommand> commands = new ArrayList<>();
        private boolean fails;

        @Override
        public void publish(ChargingEventPublishCommand command) {
            commands.add(command);
            if (fails) {
                throw new ChargingEventPublishException("broker publish failed");
            }
        }

        List<ChargingEventPublishCommand> commands() {
            return commands;
        }

        void acknowledge() {
            fails = false;
        }

        void fail() {
            fails = true;
        }

        void reset() {
            commands.clear();
            fails = false;
        }
    }
}
