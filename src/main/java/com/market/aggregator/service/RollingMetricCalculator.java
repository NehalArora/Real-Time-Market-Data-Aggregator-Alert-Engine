package com.market.aggregator.service;

import com.market.aggregator.model.MarketTick;
import com.market.aggregator.model.RollingMetric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RollingMetricCalculator {

    @Value("${market.metrics.window-minutes:5}")
    private int windowMinutes;

    // Per-symbol deque holding historical ticks within rolling window
    private final Map<String, Deque<MarketTick>> symbolTickQueues = new ConcurrentHashMap<>();

    public synchronized RollingMetric processTick(MarketTick tick) {
        String symbol = tick.getSymbol();
        Deque<MarketTick> queue = symbolTickQueues.computeIfAbsent(symbol, k -> new ArrayDeque<>());

        // Add new tick
        queue.addLast(tick);

        // Evict expired ticks older than (windowMinutes * 60 * 1000) ms
        long windowStartMs = tick.getTimestamp() - (windowMinutes * 60L * 1000L);
        while (!queue.isEmpty() && queue.peekFirst().getTimestamp() < windowStartMs) {
            queue.pollFirst();
        }

        // Calculate rolling SMA, min, max, and % change
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal minPrice = tick.getPrice();
        BigDecimal maxPrice = tick.getPrice();

        for (MarketTick t : queue) {
            sum = sum.add(t.getPrice());
            if (t.getPrice().compareTo(minPrice) < 0) {
                minPrice = t.getPrice();
            }
            if (t.getPrice().compareTo(maxPrice) > 0) {
                maxPrice = t.getPrice();
            }
        }

        int count = queue.size();
        BigDecimal sma = sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);

        // Calculate % change relative to oldest tick in current window
        MarketTick oldestTick = queue.peekFirst();
        BigDecimal oldestPrice = (oldestTick != null) ? oldestTick.getPrice() : tick.getPrice();
        BigDecimal priceChangePct = BigDecimal.ZERO;

        if (oldestPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal priceDiff = tick.getPrice().subtract(oldestPrice);
            priceChangePct = priceDiff.multiply(BigDecimal.valueOf(100))
                    .divide(oldestPrice, 4, RoundingMode.HALF_UP);
        }

        return new RollingMetric(
                symbol,
                windowMinutes,
                tick.getPrice(),
                sma,
                priceChangePct,
                minPrice,
                maxPrice,
                count,
                tick.getTimestamp()
        );
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }
}
