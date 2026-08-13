package com.example.charging.repository;

import com.example.charging.domain.ChargingEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingEventRepository extends JpaRepository<ChargingEvent, Long> {

    boolean existsByEventId(String eventId);

    List<ChargingEvent> findBySessionIdOrderBySequenceAsc(String sessionId);
}
