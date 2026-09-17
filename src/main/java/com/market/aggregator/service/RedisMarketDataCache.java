package com.market.aggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.aggregator.model.MarketTick;
import com.market.aggregator.model.RollingMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisMarketDataCache {

    private static final Logger logger = LoggerFactory.getLogger(RedisMarketDataCache.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // In-memory fallback maps when Redis server is unreachable
    private final Map<String, MarketTick> inMemoryTickCache = new ConcurrentHashMap<>();
    private final Map<String, RollingMetric> inMemoryMetricCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public RedisMarketDataCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheLatestPrice(MarketTick tick) {
        // Always populate in-memory fallback cache first (sub-millisecond guarantee)
        inMemoryTickCache.put(tick.getSymbol(), tick);

        if (redisTemplate != null) {
            try {
                String key = "price:latest:" + tick.getSymbol();
                String value = objectMapper.writeValueAsString(tick);
                redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
            } catch (Exception e) {
                logger.debug("Redis cache write bypassed, using in-memory fallback: {}", e.getMessage());
            }
        }
    }

    public MarketTick getLatestPrice(String symbol) {
        if (redisTemplate != null) {
            try {
                String key = "price:latest:" + symbol;
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    return objectMapper.readValue(json, MarketTick.class);
                }
            } catch (Exception e) {
                logger.debug("Redis read bypassed, falling back to in-memory cache: {}", e.getMessage());
            }
        }
        return inMemoryTickCache.get(symbol);
    }

    public void cacheRollingMetric(RollingMetric metric) {
        inMemoryMetricCache.put(metric.getSymbol(), metric);

        if (redisTemplate != null) {
            try {
                String key = "metrics:latest:" + metric.getSymbol();
                String value = objectMapper.writeValueAsString(metric);
                redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
            } catch (Exception e) {
                logger.debug("Redis metric cache write bypassed: {}", e.getMessage());
            }
        }
    }

    public RollingMetric getLatestMetric(String symbol) {
        if (redisTemplate != null) {
            try {
                String key = "metrics:latest:" + symbol;
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    return objectMapper.readValue(json, RollingMetric.class);
                }
            } catch (Exception e) {
                logger.debug("Redis metric read bypassed: {}", e.getMessage());
            }
        }
        return inMemoryMetricCache.get(symbol);
    }
}
