package com.market.aggregator.controller;

import com.market.aggregator.entity.TickEntity;
import com.market.aggregator.model.MarketTick;
import com.market.aggregator.model.RollingMetric;
import com.market.aggregator.repository.TickRepository;
import com.market.aggregator.service.KafkaTickConsumerService;
import com.market.aggregator.service.RedisMarketDataCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class MarketDataController {

    private final RedisMarketDataCache redisCache;
    private final TickRepository tickRepository;
    private final KafkaTickConsumerService consumerService;

    @Autowired
    public MarketDataController(RedisMarketDataCache redisCache,
                                @Autowired(required = false) TickRepository tickRepository,
                                KafkaTickConsumerService consumerService) {
        this.redisCache = redisCache;
        this.tickRepository = tickRepository;
        this.consumerService = consumerService;
    }

    @GetMapping("/latest")
    public ResponseEntity<MarketTick> getLatestPrice(@RequestParam(defaultValue = "BTCUSDT") String symbol) {
        MarketTick tick = redisCache.getLatestPrice(symbol);
        if (tick == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(tick);
    }

    @GetMapping("/metrics")
    public ResponseEntity<RollingMetric> getLatestMetrics(@RequestParam(defaultValue = "BTCUSDT") String symbol) {
        RollingMetric metric = redisCache.getLatestMetric(symbol);
        if (metric == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(metric);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TickEntity>> getTickHistory(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "50") int limit) {
        if (tickRepository == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<TickEntity> history = tickRepository.findBySymbolOrderByTimestampDesc(symbol, PageRequest.of(0, Math.min(limit, 500)));
        return ResponseEntity.ok(history);
    }

    @PostMapping("/ticks/inject")
    public ResponseEntity<String> injectTick(@RequestBody MarketTick tick) {
        if (tick.getTimestamp() <= 0) {
            tick.setTimestamp(System.currentTimeMillis());
        }
        if (tick.getSource() == null) {
            tick.setSource("REST_INJECTED");
        }
        consumerService.processIncomingTick(tick);
        return ResponseEntity.ok("Tick injected and processed successfully.");
    }
}
