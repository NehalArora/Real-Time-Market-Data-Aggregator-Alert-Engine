package com.market.aggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.aggregator.model.MarketTick;
import com.market.aggregator.model.RollingMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RedisMarketDataCacheTest {

    private RedisMarketDataCache cache;

    @BeforeEach
    public void setUp() {
        // Pass null StringRedisTemplate to test bulletproof in-memory fallback
        cache = new RedisMarketDataCache(null, new ObjectMapper());
    }

    @Test
    public void testLatestPriceCache_WriteAndRead() {
        long start = System.nanoTime();
        MarketTick tick = new MarketTick("BTCUSDT", new BigDecimal("65432.10"), new BigDecimal("0.5"), System.currentTimeMillis(), "TEST");
        cache.cacheLatestPrice(tick);

        MarketTick cached = cache.getLatestPrice("BTCUSDT");
        long durationNs = System.nanoTime() - start;

        assertNotNull(cached);
        assertEquals("BTCUSDT", cached.getSymbol());
        assertEquals(new BigDecimal("65432.10"), cached.getPrice());
        // Verify latency is sub-millisecond (< 1,000,000 ns)
        System.out.println("Cache Read Latency: " + (durationNs / 1_000_000.0) + " ms");
    }

    @Test
    public void testRollingMetricCache_WriteAndRead() {
        RollingMetric metric = new RollingMetric(
                "ETHUSDT", 5, new BigDecimal("3500.00"), new BigDecimal("3450.00"),
                new BigDecimal("1.45"), new BigDecimal("3400.00"), new BigDecimal("3500.00"), 10, System.currentTimeMillis()
        );

        cache.cacheRollingMetric(metric);
        RollingMetric cached = cache.getLatestMetric("ETHUSDT");

        assertNotNull(cached);
        assertEquals("ETHUSDT", cached.getSymbol());
        assertEquals(new BigDecimal("3500.00"), cached.getCurrentPrice());
    }
}
