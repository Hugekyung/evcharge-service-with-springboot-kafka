package com.example.charging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.charging.domain.ChargingEventType;
import com.example.charging.domain.ChargingSession;
import com.example.charging.domain.ChargingSessionStatus;
import com.example.charging.kafka.ChargingEventMessage;
import com.example.charging.repository.ChargingEventRepository;
import com.example.charging.repository.ChargingSessionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChargingSessionServiceImplTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-12T03:00:00Z");
    private static final Instant PROCESSED_AT = Instant.parse("2026-08-12T03:00:01Z");

    @Mock
    private ChargingSessionRepository sessionRepository;

    @Mock
    private ChargingEventRepository eventRepository;

    private ChargingSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChargingSessionServiceImpl(
                sessionRepository,
                eventRepository,
                Clock.fixed(PROCESSED_AT, ZoneOffset.UTC));
    }

    @Test
    void createsSessionAndStoresStartedEvent() {
        ChargingEventMessage message = message("evt-1", ChargingEventType.CHARGING_STARTED, 1);
        when(eventRepository.existsByEventId("evt-1")).thenReturn(false);
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ChargingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.process(message);

        verify(sessionRepository).save(any(ChargingSession.class));
        verify(eventRepository).save(any());
    }

    @Test
    void processesTheSameEventIdOnlyOnce() {
        ChargingEventMessage message = message("evt-1", ChargingEventType.CHARGING_STARTED, 1);
        when(eventRepository.existsByEventId("evt-1")).thenReturn(false, true);
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ChargingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.process(message);
        service.process(message);

        verify(sessionRepository).findBySessionId("session-1");
        verify(sessionRepository).save(any(ChargingSession.class));
        verify(eventRepository).save(any());
    }

    @Test
    void storesOlderNewEventWithoutChangingSessionSequence() {
        ChargingSession session = ChargingSession.start(
                "session-1", "charger-1", 3, 40, BigDecimal.ONE, OCCURRED_AT, PROCESSED_AT);
        when(eventRepository.existsByEventId("evt-2")).thenReturn(false);
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));

        service.process(message("evt-2", ChargingEventType.CHARGING_PROGRESS, 2));

        assertThat(session.getLastSequence()).isEqualTo(3);
        assertThat(session.getStatus()).isEqualTo(ChargingSessionStatus.CHARGING);
        verify(sessionRepository, never()).save(any());
        verify(eventRepository).save(any());
    }

    @Test
    void rejectsNonStartedEventWhenSessionDoesNotExist() {
        when(eventRepository.existsByEventId("evt-3")).thenReturn(false);
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(message("evt-3", ChargingEventType.CHARGING_PROGRESS, 1)))
                .isInstanceOf(ChargingEventBusinessException.class);
    }

    private ChargingEventMessage message(String eventId, ChargingEventType eventType, long sequence) {
        return new ChargingEventMessage(
                eventId,
                "charger-1",
                "session-1",
                eventType,
                sequence,
                40,
                BigDecimal.ONE,
                OCCURRED_AT);
    }
}
