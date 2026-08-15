package com.example.charging.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.charging.domain.ChargingEventType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerializer;

class ChargingEventMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("Kafka 메시지의 모든 필드를 문서화된 JSON 타입으로 직렬화한다")
    void serializesEveryDocumentedKafkaPayloadFieldWithItsExpectedJsonType() throws Exception {
        // Given
        ChargingEventMessage message = new ChargingEventMessage(
                "evt-100001",
                "charger-001",
                "session-001",
                ChargingEventType.CHARGING_STARTED,
                1,
                35,
                new BigDecimal("12.50"),
                Instant.parse("2026-08-12T03:00:00Z"));

        // When
        JsonNode payload = objectMapper.readTree(objectMapper.writeValueAsBytes(message));

        // Then
        assertThat(payload.properties())
                .extracting(java.util.Map.Entry::getKey)
                .containsExactlyInAnyOrder(
                        "eventId",
                        "chargerId",
                        "sessionId",
                        "eventType",
                        "sequence",
                        "batteryLevel",
                        "chargedKwh",
                        "occurredAt");
        assertThat(payload.get("eventId").isTextual()).isTrue();
        assertThat(payload.get("eventId").textValue()).isEqualTo("evt-100001");
        assertThat(payload.get("chargerId").isTextual()).isTrue();
        assertThat(payload.get("chargerId").textValue()).isEqualTo("charger-001");
        assertThat(payload.get("sessionId").isTextual()).isTrue();
        assertThat(payload.get("sessionId").textValue()).isEqualTo("session-001");
        assertThat(payload.get("eventType").isTextual()).isTrue();
        assertThat(payload.get("eventType").textValue()).isEqualTo("CHARGING_STARTED");
        assertThat(payload.get("sequence").isIntegralNumber()).isTrue();
        assertThat(payload.get("sequence").longValue()).isEqualTo(1L);
        assertThat(payload.get("batteryLevel").isIntegralNumber()).isTrue();
        assertThat(payload.get("batteryLevel").intValue()).isEqualTo(35);
        assertThat(payload.get("chargedKwh").isNumber()).isTrue();
        assertThat(payload.get("chargedKwh").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(payload.get("occurredAt").isTextual()).isTrue();
        assertThat(payload.get("occurredAt").textValue()).isEqualTo("2026-08-12T03:00:00Z");
    }

    @Test
    @DisplayName("Kafka JsonSerializer가 occurredAt을 ISO-8601 문자열로 직렬화한다")
    void configuredKafkaJsonSerializerWritesOccurredAtAsIso8601Text() throws Exception {
        // Given
        ChargingEventMessage message = new ChargingEventMessage(
                "evt-100001",
                "charger-001",
                "session-001",
                ChargingEventType.CHARGING_STARTED,
                1,
                35,
                new BigDecimal("12.50"),
                Instant.parse("2026-08-12T03:00:00Z"));
        JsonSerializer<ChargingEventMessage> serializer = new JsonSerializer<>();

        // When
        JsonNode payload = objectMapper.readTree(serializer.serialize("charging-events", message));

        // Then
        assertThat(payload.get("occurredAt").isTextual()).isTrue();
        assertThat(payload.get("occurredAt").textValue()).isEqualTo("2026-08-12T03:00:00Z");
    }
}
