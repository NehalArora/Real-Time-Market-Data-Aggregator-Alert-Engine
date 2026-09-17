package com.market.aggregator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.aggregator.model.MarketTick;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BinanceWebSocketIngestionService implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketIngestionService.class);

    private final KafkaProducerService kafkaProducerService;
    private final KafkaTickConsumerService kafkaTickConsumerService;
    private final ObjectMapper objectMapper;
    private WebSocketSession webSocketSession;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final Random random = new Random();

    @Value("${market.binance.websocket-url:wss://stream.binance.com:9443/ws/btcusdt@ticker}")
    private String websocketUrl;

    @Value("${market.binance.symbol:BTCUSDT}")
    private String defaultSymbol;

    private BigDecimal simulatedPrice = new BigDecimal("65000.00");

    public BinanceWebSocketIngestionService(KafkaProducerService kafkaProducerService,
                                            KafkaTickConsumerService kafkaTickConsumerService,
                                            ObjectMapper objectMapper) {
        this.kafkaProducerService = kafkaProducerService;
        this.kafkaTickConsumerService = kafkaTickConsumerService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void connect() {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            logger.info("Connecting to Binance WebSocket endpoint: {}", websocketUrl);
            client.execute(this, websocketUrl).get();
        } catch (Exception e) {
            logger.warn("Could not establish initial Binance WebSocket connection: {}. Will use fallback tick generator if needed.", e.getMessage());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.webSocketSession = session;
        this.isConnected.set(true);
        logger.info("Successfully connected to Binance WebSocket!");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            if (message instanceof TextMessage textMessage) {
                String payload = textMessage.getPayload();
                JsonNode root = objectMapper.readTree(payload);

                // Parse Binance Ticker payload format:
                // "s": Symbol, "c": Last price, "q": Volume/Quantity, "E": Event time
                String symbol = root.has("s") ? root.get("s").asText() : defaultSymbol;
                String priceStr = root.has("c") ? root.get("c").asText() : (root.has("p") ? root.get("p").asText() : "0.0");
                String qtyStr = root.has("q") ? root.get("q").asText() : (root.has("v") ? root.get("v").asText() : "1.0");
                long eventTime = root.has("E") ? root.get("E").asLong() : System.currentTimeMillis();

                BigDecimal price = new BigDecimal(priceStr);
                BigDecimal quantity = new BigDecimal(qtyStr);

                MarketTick tick = new MarketTick(symbol, price, quantity, eventTime, "BINANCE_WEBSOCKET");
                kafkaProducerService.sendTick(tick);
                kafkaTickConsumerService.processIncomingTick(tick);
            }
        } catch (Exception e) {
            logger.error("Error processing WebSocket payload: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.warn("Binance WebSocket transport error: {}", exception.getMessage());
        isConnected.set(false);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        logger.info("Binance WebSocket closed: {}", closeStatus);
        isConnected.set(false);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // Fallback tick generator runs every 2 seconds if WebSocket is disconnected or in local test environment
    @Scheduled(fixedRate = 2000)
    public void generateFallbackTick() {
        if (!isConnected.get()) {
            double changePercent = (random.nextDouble() - 0.49) * 0.01; // +/- 0.5% variance
            simulatedPrice = simulatedPrice.multiply(BigDecimal.valueOf(1.0 + changePercent)).setScale(2, RoundingMode.HALF_UP);
            MarketTick tick = new MarketTick(defaultSymbol, simulatedPrice, new BigDecimal("0.5"), System.currentTimeMillis(), "SIMULATED_FEED");
            logger.info("[FALLBACK FEED] Generating live tick: {} = ${}", tick.getSymbol(), tick.getPrice());
            kafkaProducerService.sendTick(tick);
            kafkaTickConsumerService.processIncomingTick(tick);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            try {
                webSocketSession.close();
            } catch (Exception ignored) {}
        }
    }

    public boolean isConnected() {
        return isConnected.get();
    }
}
