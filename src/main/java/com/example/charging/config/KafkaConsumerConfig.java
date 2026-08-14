package com.example.charging.config;

import com.example.charging.application.ChargingEventBusinessException;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.BackOffHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.apache.kafka.common.TopicPartition;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(record.topic() + "-dlt", record.partition()));

        return createErrorHandler(recoverer);
    }

    static DefaultErrorHandler createErrorHandler(ConsumerRecordRecoverer recoverer) {
        return createErrorHandler(recoverer, null);
    }

    static DefaultErrorHandler createErrorHandler(
            ConsumerRecordRecoverer recoverer,
            BackOffHandler backOffHandler) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(2);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(16_000);

        DefaultErrorHandler errorHandler = backOffHandler == null
                ? new DefaultErrorHandler(recoverer, backOff)
                : new DefaultErrorHandler(recoverer, backOff, backOffHandler);
        errorHandler.defaultFalse();
        errorHandler.addRetryableExceptions(
                TransientDataAccessException.class,
                DataAccessResourceFailureException.class,
                RetriableException.class);
        errorHandler.addNotRetryableExceptions(ChargingEventBusinessException.class);
        return errorHandler;
    }
}
