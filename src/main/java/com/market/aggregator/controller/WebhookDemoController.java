package com.market.aggregator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookDemoController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDemoController.class);

    private final List<Map<String, Object>> receivedAlerts = Collections.synchronizedList(new ArrayList<>());

    @PostMapping("/alert-receiver")
    public ResponseEntity<Map<String, String>> receiveAlertWebhook(@RequestBody Map<String, Object> alertPayload) {
        logger.info("🔔 [WEBHOOK RECEIVER] Received Real-Time Alert Event Payload: {}", alertPayload);
        receivedAlerts.add(alertPayload);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Alert payload received"));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> getReceivedAlerts() {
        return ResponseEntity.ok(receivedAlerts);
    }
}
