package com.example.charging.application;

import com.example.charging.domain.ChargingEvent;
import com.example.charging.domain.ChargingEventType;
import com.example.charging.domain.ChargingSession;
import com.example.charging.domain.ChargingSessionStatus;
import com.example.charging.kafka.ChargingEventMessage;
import com.example.charging.repository.ChargingEventRepository;
import com.example.charging.repository.ChargingSessionRepository;
import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargingSessionServiceImpl implements ChargingSessionService {

    private final ChargingSessionRepository sessionRepository;
    private final ChargingEventRepository eventRepository;
    private final Clock clock;

    @Autowired
    public ChargingSessionServiceImpl(
            ChargingSessionRepository sessionRepository,
            ChargingEventRepository eventRepository) {
        this(sessionRepository, eventRepository, Clock.systemUTC());
    }

    // 테스트용 생성자(시간 고정용)
    ChargingSessionServiceImpl(
            ChargingSessionRepository sessionRepository,
            ChargingEventRepository eventRepository,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ChargingSession getBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ChargingSessionNotFoundException(sessionId));
    }

    @Override
    @Transactional
    public void process(ChargingEventMessage message) {
        if (eventRepository.existsByEventId(message.eventId())) {
            return; // 같은 eventId가 DB에 이미 존재하면 그 즉시 종료(중복 처리 x)
        }

        Instant now = clock.instant();
        ChargingSession session = sessionRepository.findBySessionId(message.sessionId())
                .orElseGet(() -> createSession(message, now));

        validateCharger(session, message);
        if (session.getLastSequence() < message.sequence()) {
            applyTransition(session, message, now);
            sessionRepository.save(session); // 오래된 sequence는 이력만 저장
        }

        eventRepository.save(ChargingEvent.create(
                message.eventId(),
                message.sessionId(),
                message.chargerId(),
                message.eventType(),
                message.sequence(),
                message.batteryLevel(),
                message.chargedKwh(),
                message.occurredAt(),
                now));
    }

    private ChargingSession createSession(ChargingEventMessage message, Instant now) {
        if (message.eventType() != ChargingEventType.CHARGING_STARTED) {
            throw new ChargingEventBusinessException("Session does not exist for event");
        }

        // ChargingEventType이 CHARGING_STARTED 이면 생성
        ChargingSession session = ChargingSession.start(
                message.sessionId(),
                message.chargerId(),
                message.sequence(),
                message.batteryLevel(),
                message.chargedKwh(),
                message.occurredAt(),
                now);
        return sessionRepository.save(session);
    }

    private void validateCharger(ChargingSession session, ChargingEventMessage message) {
        if (!session.getChargerId().equals(message.chargerId())) {
            throw new ChargingEventBusinessException("Event charger does not match session");
        }
    }

    private void applyTransition(ChargingSession session, ChargingEventMessage message, Instant now) {
        try {
            switch (message.eventType()) {
                case CHARGING_STARTED -> throw new ChargingEventBusinessException("Session already exists");
                case CHARGING_PROGRESS -> session.applyProgress(
                        message.sequence(), message.batteryLevel(), message.chargedKwh(), now);
                case CHARGING_COMPLETED -> session.complete(
                        message.sequence(), message.batteryLevel(), message.chargedKwh(), message.occurredAt(), now);
                case CHARGING_FAILED -> session.fail(
                        message.sequence(), message.batteryLevel(), message.chargedKwh(), message.occurredAt(), now);
            }
        } catch (IllegalStateException exception) {
            throw new ChargingEventBusinessException(exception.getMessage());
        }
    }
}
