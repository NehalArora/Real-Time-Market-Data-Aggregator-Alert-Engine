package com.market.aggregator.service;

import com.market.aggregator.model.RollingMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertEngineService {

    private static final Logger logger = LoggerFactory.getLogger(AlertEngineService.class);

    private final RestTemplate restTemplate;

    @Value("${market.alert.enabled:true}")
    private boolean alertEnabled;

    @Value("${market.alert.price-pct-change-threshold:2.0}")
    private double pctThreshold;

    @Value("${market.alert.webhook-url:http://localhost:8080/api/v1/webhook/alert-receiver}")
    private String webhookUrl;

    // Cooldown map: symbol -> last alert timestamp
    private final Map<String, Long> lastAlertTimeMap = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 30000; // 30s cooldown per symbol

    public AlertEngineService() {
        this.restTemplate = new RestTemplate();
    }

    public AlertEngineService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean evaluateAndAlert(RollingMetric metric) {
        if (!alertEnabled || metric == null || metric.getPriceChangePct() == null) {
            return false;
        }

        BigDecimal pctChange = metric.getPriceChangePct().abs();
        if (pctChange.doubleValue() >= pctThreshold) {
            long now = System.currentTimeMillis();
            Long lastAlert = lastAlertTimeMap.get(metric.getSymbol());

            if (lastAlert == null || (now - lastAlert) > COOLDOWN_MS) {
                lastAlertTimeMap.put(metric.getSymbol(), now);
                logger.warn("🚨 [THRESHOLD BREACH DETECTED] Symbol: {}, % Change in {}m: {}% (Threshold: {}%), Price: ${}",
                        metric.getSymbol(), metric.getWindowMinutes(), metric.getPriceChangePct(), pctThreshold, metric.getCurrentPrice());

                dispatchWebhookAlert(metric);
                return true;
            }
        }
        return false;
    }

    @Async
    public void dispatchWebhookAlert(RollingMetric metric) {
        try {
            Map<String, Object> alertPayload = new HashMap<>();
            alertPayload.put("event", "PRICE_MOVEMENT_ALERT");
            alertPayload.put("symbol", metric.getSymbol());
            alertPayload.put("priceChangePct", metric.getPriceChangePct());
            alertPayload.put("currentPrice", metric.getCurrentPrice());
            alertPayload.put("sma", metric.getSma());
            alertPayload.put("windowMinutes", metric.getWindowMinutes());
            alertPayload.put("thresholdPct", pctThreshold);
            alertPayload.put("timestamp", metric.getTimestamp());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(alertPayload, headers);

            logger.info("Sending Webhook Alert POST to {}", webhookUrl);
            restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (Exception e) {
            logger.warn("Webhook POST delivery skipped or failed: {}", e.getMessage());
        }
    }

    public void setPctThreshold(double pctThreshold) {
        this.pctThreshold = pctThreshold;
    }

    public double getPctThreshold() {
        return pctThreshold;
    }

    public void setAlertEnabled(boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
