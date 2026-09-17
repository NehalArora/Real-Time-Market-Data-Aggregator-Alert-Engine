package com.market.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MarketDataAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataAggregatorApplication.class, args);
    }
}
