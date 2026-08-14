package com.example.charging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "charging_event")
public class ChargingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "charger_id", nullable = false)
    private String chargerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ChargingEventType eventType;

    @Column(name = "sequence", nullable = false)
    private long sequence;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "charged_kwh", precision = 12, scale = 3)
    private BigDecimal chargedKwh;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ChargingEvent() {
    }

    public static ChargingEvent create(
            String eventId,
            String sessionId,
            String chargerId,
            ChargingEventType eventType,
            long sequence,
            Integer batteryLevel,
            BigDecimal chargedKwh,
            Instant occurredAt,
            Instant processedAt) {
        ChargingEvent event = new ChargingEvent();
        event.eventId = eventId;
        event.sessionId = sessionId;
        event.chargerId = chargerId;
        event.eventType = eventType;
        event.sequence = sequence;
        event.batteryLevel = batteryLevel;
        event.chargedKwh = chargedKwh;
        event.occurredAt = occurredAt;
        event.processedAt = processedAt;
        return event;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getChargerId() {
        return chargerId;
    }

    public ChargingEventType getEventType() {
        return eventType;
    }

    public long getSequence() {
        return sequence;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public BigDecimal getChargedKwh() {
        return chargedKwh;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
