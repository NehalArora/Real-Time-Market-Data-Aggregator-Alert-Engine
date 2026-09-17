package com.market.aggregator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "rolling_metrics", indexes = {
        @Index(name = "idx_metric_symbol_ts", columnList = "symbol, timestamp DESC")
})
public class RollingMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private Integer windowMinutes;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal sma;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal priceChangePct;

    @Column(precision = 18, scale = 8)
    private BigDecimal minPrice;

    @Column(precision = 18, scale = 8)
    private BigDecimal maxPrice;

    @Column(nullable = false)
    private Integer tickCount;

    @Column(nullable = false)
    private Long timestamp;

    public RollingMetricEntity() {
    }

    public RollingMetricEntity(String symbol, Integer windowMinutes, BigDecimal currentPrice, BigDecimal sma,
                               BigDecimal priceChangePct, BigDecimal minPrice, BigDecimal maxPrice,
                               Integer tickCount, Long timestamp) {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(Integer windowMinutes) {
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

    public Integer getTickCount() {
        return tickCount;
    }

    public void setTickCount(Integer tickCount) {
        this.tickCount = tickCount;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
