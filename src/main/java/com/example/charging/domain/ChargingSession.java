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
@Table(name = "charging_session")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "charger_id", nullable = false)
    private String chargerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChargingSessionStatus status;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "charged_kwh", precision = 12, scale = 3)
    private BigDecimal chargedKwh;

    @Column(name = "last_sequence", nullable = false)
    private long lastSequence;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChargingSession() {
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getChargerId() {
        return chargerId;
    }

    public ChargingSessionStatus getStatus() {
        return status;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public BigDecimal getChargedKwh() {
        return chargedKwh;
    }

    public long getLastSequence() {
        return lastSequence;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
