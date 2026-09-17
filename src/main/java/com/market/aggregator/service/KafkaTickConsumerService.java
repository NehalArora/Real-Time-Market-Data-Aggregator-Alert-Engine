package com.market.aggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.aggregator.entity.RollingMetricEntity;
import com.market.aggregator.entity.TickEntity;
import com.market.aggregator.model.MarketTick;
import com.market.aggregator.model.RollingMetric;
import com.market.aggregator.repository.RollingMetricRepository;
import com.market.aggregator.repository.TickRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class KafkaTickConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTickConsumerService.class);

    private final ObjectMapper objectMapper;
    private final RedisMarketDataCache redisCache;
    private final RollingMetricCalculator metricCalculator;
    private final AlertEngineService alertEngineService;
    private final TickRepository tickRepository;
    private final RollingMetricRepository metricRepository;

    @Autowired
    public KafkaTickConsumerService(ObjectMapper objectMapper,
                                    RedisMarketDataCache redisCache,
                                    RollingMetricCalculator metricCalculator,
                                    AlertEngineService alertEngineService,
                                    @Autowired(required = false) TickRepository tickRepository,
                                    @Autowired(required = false) RollingMetricRepository metricRepository) {
        this.objectMapper = objectMapper;
        this.redisCache = redisCache;
        this.metricCalculator = metricCalculator;
        this.alertEngineService = alertEngineService;
        this.tickRepository = tickRepository;
        this.metricRepository = metricRepository;
    }

    @KafkaListener(topics = "${market.kafka.topic:market-ticks}", groupId = "${spring.kafka.consumer.group-id:market-aggregator-group}")
    public void consumeTick(String message) {
        try {
            MarketTick tick = objectMapper.readValue(message, MarketTick.class);
            processIncomingTick(tick);
        } catch (Exception e) {
            logger.error("Error consuming Kafka tick message: {}", e.getMessage());
        }
    }

    public void processIncomingTick(MarketTick tick) {
        logger.debug("Processing tick for {}: ${}", tick.getSymbol(), tick.getPrice());

        // 1. Update sub-millisecond Redis cache
        redisCache.cacheLatestPrice(tick);

        // 2. Compute 5-minute rolling metrics
        RollingMetric metric = metricCalculator.processTick(tick);

        // 3. Cache latest metric
        redisCache.cacheRollingMetric(metric);

        // 4. Async database persistence (PostgreSQL / H2)
        persistTickAndMetricAsync(tick, metric);

        // 5. Evaluate threshold alerting
        alertEngineService.evaluateAndAlert(metric);
    }

    @Async
    public void persistTickAndMetricAsync(MarketTick tick, RollingMetric metric) {
        try {
            if (tickRepository != null) {
                TickEntity tickEntity = new TickEntity(
                        tick.getSymbol(),
                        tick.getPrice(),
                        tick.getQuantity(),
                        tick.getTimestamp(),
                        tick.getSource()
                );
                tickRepository.save(tickEntity);
            }

            if (metricRepository != null) {
                RollingMetricEntity metricEntity = new RollingMetricEntity(
                        metric.getSymbol(),
                        metric.getWindowMinutes(),
                        metric.getCurrentPrice(),
                        metric.getSma(),
                        metric.getPriceChangePct(),
                        metric.getMinPrice(),
                        metric.getMaxPrice(),
                        metric.getTickCount(),
                        metric.getTimestamp()
                );
                metricRepository.save(metricEntity);
            }
        } catch (Exception e) {
            logger.warn("Database persistence note: {}", e.getMessage());
        }
    }
}
