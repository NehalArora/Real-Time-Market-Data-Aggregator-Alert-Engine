package com.market.aggregator.service;

import com.market.aggregator.model.MarketTick;
import com.market.aggregator.model.RollingMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RollingMetricCalculatorTest {

    private RollingMetricCalculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new RollingMetricCalculator();
        calculator.setWindowMinutes(5);
    }

    @Test
    public void testProcessTick_SingleTick() {
        long now = System.currentTimeMillis();
        MarketTick tick = new MarketTick("BTCUSDT", new BigDecimal("50000.00"), new BigDecimal("1.0"), now, "TEST");

        RollingMetric metric = calculator.processTick(tick);

        assertNotNull(metric);
        assertEquals("BTCUSDT", metric.getSymbol());
        assertEquals(5, metric.getWindowMinutes());
        assertEquals(new BigDecimal("50000.00"), metric.getCurrentPrice());
        assertEquals(new BigDecimal("50000.0000"), metric.getSma());
        assertEquals(new BigDecimal("0.0000"), metric.getPriceChangePct());
        assertEquals(1, metric.getTickCount());
    }

    @Test
    public void testProcessTick_MultipleTicks_SmaAndPctChange() {
        long now = System.currentTimeMillis();

        // Tick 1: $100.00
        MarketTick tick1 = new MarketTick("BTCUSDT", new BigDecimal("100.00"), new BigDecimal("1.0"), now - 60000, "TEST");
        calculator.processTick(tick1);

        // Tick 2: $110.00 (+10% move from $100)
        MarketTick tick2 = new MarketTick("BTCUSDT", new BigDecimal("110.00"), new BigDecimal("1.0"), now, "TEST");
        RollingMetric metric = calculator.processTick(tick2);

        // SMA of 100 & 110 = 105.0000
        assertEquals(new BigDecimal("105.0000"), metric.getSma());
        // % Change from 100 to 110 = +10.0000%
        assertEquals(new BigDecimal("10.0000"), metric.getPriceChangePct());
        assertEquals(2, metric.getTickCount());
    }

    @Test
    public void testProcessTick_ExpiredTicksEviction() {
        long now = System.currentTimeMillis();
        long sixMinutesAgo = now - (6 * 60 * 1000);

        // Tick 1: 6 minutes ago ($100) -> Out of 5 min window
        MarketTick tickOld = new MarketTick("BTCUSDT", new BigDecimal("100.00"), new BigDecimal("1.0"), sixMinutesAgo, "TEST");
        calculator.processTick(tickOld);

        // Tick 2: Now ($200) -> Only this tick should be in 5 min window
        MarketTick tickNew = new MarketTick("BTCUSDT", new BigDecimal("200.00"), new BigDecimal("1.0"), now, "TEST");
        RollingMetric metric = calculator.processTick(tickNew);

        assertEquals(1, metric.getTickCount());
        assertEquals(new BigDecimal("200.0000"), metric.getSma());
    }
}
