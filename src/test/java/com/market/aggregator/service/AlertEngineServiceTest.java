package com.market.aggregator.service;

import com.market.aggregator.model.RollingMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AlertEngineServiceTest {

    private AlertEngineService alertEngineService;
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        alertEngineService = new AlertEngineService(restTemplate);
        alertEngineService.setAlertEnabled(true);
        alertEngineService.setPctThreshold(2.0); // 2% threshold
    }

    @Test
    public void testEvaluateAndAlert_BreachedThreshold() {
        RollingMetric metric = new RollingMetric(
                "BTCUSDT", 5, new BigDecimal("103.00"), new BigDecimal("101.50"),
                new BigDecimal("3.0000"), // +3.0% move (exceeds 2.0% threshold)
                new BigDecimal("100.00"), new BigDecimal("103.00"), 5, System.currentTimeMillis()
        );

        boolean triggered = alertEngineService.evaluateAndAlert(metric);

        assertTrue(triggered);
    }

    @Test
    public void testEvaluateAndAlert_BelowThreshold() {
        RollingMetric metric = new RollingMetric(
                "BTCUSDT", 5, new BigDecimal("101.00"), new BigDecimal("100.50"),
                new BigDecimal("1.0000"), // +1.0% move (below 2.0% threshold)
                new BigDecimal("100.00"), new BigDecimal("101.00"), 5, System.currentTimeMillis()
        );

        boolean triggered = alertEngineService.evaluateAndAlert(metric);

        assertFalse(triggered);
    }
}
