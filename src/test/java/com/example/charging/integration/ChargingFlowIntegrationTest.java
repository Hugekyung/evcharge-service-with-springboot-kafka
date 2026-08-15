package com.example.charging.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.charging.application.ChargingEventPublishCommand;
import com.example.charging.domain.ChargingEventType;
import com.example.charging.domain.ChargingSessionStatus;
import com.example.charging.kafka.ChargingEventProducer;
import com.example.charging.repository.ChargingEventRepository;
import com.example.charging.repository.ChargingSessionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ChargingFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("evcharging")
            .withUsername("evcharging")
            .withPassword("evcharging");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @Autowired
    private ChargingEventProducer producer;

    @Autowired
    private ChargingSessionRepository sessionRepository;

    @Autowired
    private ChargingEventRepository eventRepository;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    private KafkaTemplate<String, ?> kafkaTemplate;

    @AfterEach
    void stopListenersBeforeContainers() {
        listenerRegistry.stop();
        ((DefaultKafkaProducerFactory<?, ?>) kafkaTemplate.getProducerFactory()).destroy();
    }

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Test
    @DisplayName("Kafka 이벤트가 Consumer를 거쳐 PostgreSQL에 Session과 이력으로 저장된다")
    void publishesConsumesAndPersistsChargingFlow() {
        String sessionId = "integration-session-" + System.nanoTime();
        Instant occurredAt = Instant.parse("2026-08-12T03:00:00Z");

        producer.publish(command("evt-started-" + sessionId, sessionId,
                ChargingEventType.CHARGING_STARTED, 1, 35, BigDecimal.ZERO, occurredAt));
        producer.publish(command("evt-progress-" + sessionId, sessionId,
                ChargingEventType.CHARGING_PROGRESS, 2, 55, new BigDecimal("5.00"), occurredAt.plusSeconds(1)));
        producer.publish(command("evt-completed-" + sessionId, sessionId,
                ChargingEventType.CHARGING_COMPLETED, 3, 80, new BigDecimal("10.00"), occurredAt.plusSeconds(2)));

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    var session = sessionRepository.findBySessionId(sessionId).orElseThrow();
                    assertThat(session.getStatus()).isEqualTo(ChargingSessionStatus.COMPLETED);
                    assertThat(session.getLastSequence()).isEqualTo(3);
                    assertThat(eventRepository.findBySessionIdOrderBySequenceAsc(sessionId))
                            .hasSize(3)
                            .extracting(event -> event.getSequence())
                            .containsExactly(1L, 2L, 3L);
                });
    }

    private static ChargingEventPublishCommand command(
            String eventId,
            String sessionId,
            ChargingEventType eventType,
            long sequence,
            int batteryLevel,
            BigDecimal chargedKwh,
            Instant occurredAt) {
        return new ChargingEventPublishCommand(
                eventId,
                "integration-charger",
                sessionId,
                eventType,
                sequence,
                batteryLevel,
                chargedKwh,
                occurredAt);
    }
}
