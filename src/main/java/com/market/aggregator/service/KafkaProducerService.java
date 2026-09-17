package com.market.aggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.aggregator.model.MarketTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${market.kafka.topic:market-ticks}")
    private String topic;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendTick(MarketTick tick) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(tick);
            logger.debug("Publishing tick to Kafka topic {}: {}", topic, jsonPayload);
            kafkaTemplate.send(topic, tick.getSymbol(), jsonPayload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.warn("Kafka publish failed for tick {}: {}", tick.getSymbol(), ex.getMessage());
                        } else {
                            logger.trace("Tick published successfully to Kafka partition {}", result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            logger.error("Error serializing MarketTick for Kafka: {}", e.getMessage(), e);
        }
    }
}
