package com.market.aggregator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RollingMetric {

    private String symbol;
    private int windowMinutes;
    private BigDecimal currentPrice;
    private BigDecimal sma;
    private BigDecimal priceChangePct;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int tickCount;
    private long timestamp;

    public RollingMetric() {
    }

    public RollingMetric(String symbol, int windowMinutes, BigDecimal currentPrice, BigDecimal sma,
                         BigDecimal priceChangePct, BigDecimal minPrice, BigDecimal maxPrice,
                         int tickCount, long timestamp) {
        this.symbol = symbol;
        this.windowMinutes = windowMinutes;
        this.currentPrice = currentPrice;
        this.sma = sma;
        this.priceChangePct = priceChangePct;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.tickCount = tickCount;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getSma() {
        return sma;
    }

    public void setSma(BigDecimal sma) {
        this.sma = sma;
    }

    public BigDecimal getPriceChangePct() {
        return priceChangePct;
    }

    public void setPriceChangePct(BigDecimal priceChangePct) {
        this.priceChangePct = priceChangePct;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RollingMetric{" +
                "symbol='" + symbol + '\'' +
                ", windowMinutes=" + windowMinutes +
                ", currentPrice=" + currentPrice +
                ", sma=" + sma +
                ", priceChangePct=" + priceChangePct +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", tickCount=" + tickCount +
                ", timestamp=" + timestamp +
                '}';
    }
}
