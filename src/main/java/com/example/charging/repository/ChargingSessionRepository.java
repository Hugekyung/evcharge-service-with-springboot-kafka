package com.example.charging.repository;

import com.example.charging.domain.ChargingSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    Optional<ChargingSession> findBySessionId(String sessionId);
}
