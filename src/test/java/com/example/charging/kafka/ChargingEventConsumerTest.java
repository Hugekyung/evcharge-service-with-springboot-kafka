package com.example.charging.kafka;

import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.charging.application.ChargingSessionService;
import com.example.charging.domain.ChargingEventType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ChargingEventConsumerTest {

    private static ChargingEventMessage message() {
        return new ChargingEventMessage(
                "evt-100001",
                "charger-001",
                "session-001",
                ChargingEventType.CHARGING_STARTED,
                1,
                35,
                new BigDecimal("12.50"),
                Instant.parse("2026-08-12T03:00:00Z"));
    }

    @Test
    @DisplayName("수신한 이벤트를 ChargingSessionService에 위임한다")
    void delegatesReceivedMessageToChargingSessionService() {
        ChargingSessionService service = org.mockito.Mockito.mock(ChargingSessionService.class);
        ChargingEventConsumer consumer = new ChargingEventConsumer(service);
        ChargingEventMessage message = message();

        consumer.consume(message);

        verify(service).process(message);
    }

    @Test
    @DisplayName("DLT 이벤트의 핵심 식별 정보를 로그로 남긴다")
    void logsFailedEventAtDltHandler() {
        Logger logger = (Logger) LoggerFactory.getLogger(ChargingEventConsumer.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        ChargingEventConsumer consumer = new ChargingEventConsumer(org.mockito.Mockito.mock(ChargingSessionService.class));

        try {
            consumer.handleDlt(message());

            assertThat(appender.list)
                    .anySatisfy(event -> assertThat(event.getFormattedMessage())
                            .contains("eventId=evt-100001", "sessionId=session-001", "sequence=1"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

}
