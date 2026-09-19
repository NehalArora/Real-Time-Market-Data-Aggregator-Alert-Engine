# Real-Time Market Data Aggregator & Alert Engine

A backend system that ingests live cryptocurrency price ticks, streams them through Kafka, computes rolling metrics, caches low-latency reads in Redis, persists history in PostgreSQL, and fires webhook alerts on threshold breaches.

Built as a portfolio/skills-development project demonstrating event-driven architecture, stream processing, low-latency caching, and real-time alerting.

---

## Architecture

```
Binance WebSocket (live ticks)
        │
        ▼
Ingestion Service ──▶ Kafka topic: market-ticks
                              │
                              ▼
                    Kafka Consumer (Stream Processor)
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
         Redis Cache     PostgreSQL      Alert Engine
        (latest price   (tick history,  (threshold check
         + metrics,      rolling         → webhook POST)
         sub-ms reads)   metrics)
              │               │
              └───────┬───────┘
                       ▼
              REST API (read endpoints)
```

**Flow:** live ticks → Kafka → stream processor computes a rolling SMA and % change → each tick updates the Redis cache, is persisted to PostgreSQL, and is evaluated against a threshold rule → a breach triggers an HTTP POST to a configured webhook URL.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Ingestion | Binance public WebSocket API (no key required) |
| Messaging | Apache Kafka (KRaft mode) |
| Stream processing | Java 17+, Spring Boot, Spring Kafka |
| Low-latency cache | Redis (with in-memory fallback) |
| Persistence | PostgreSQL |
| API | Spring Web (REST) |
| Infrastructure | Docker Compose |

---

## Prerequisites

- Java 17+
- Maven (or use the included wrapper — no local install needed)
- Docker & Docker Compose

---

## Getting Started

### 1. Clone and start infrastructure

```bash
git clone <repo-url>
cd real-time-market-data-aggregator
docker-compose up -d
```

This brings up Kafka, PostgreSQL, and Redis locally.

### 2. Configure

Edit `src/main/resources/application.yml` as needed:

```yaml
market:
  metrics:
    window-minutes: 5        # rolling window size
  alert:
    threshold-pct: 2.0       # % move that triggers an alert
    webhook-url: http://localhost:8080/api/v1/webhook/alert-receiver
  symbols:
    - BTCUSDT
    - ETHUSDT
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The app connects to Binance's WebSocket, publishes ticks to Kafka, and starts processing immediately. Watch the console logs to confirm ticks are flowing.

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/market/latest?symbol={symbol}` | Latest price for a symbol, served from Redis |
| GET | `/api/v1/market/metrics?symbol={symbol}` | Current rolling SMA and % change |
| GET | `/api/v1/market/history?symbol={symbol}&limit=50` | Historical ticks from PostgreSQL |
| POST | `/api/v1/webhook/alert-receiver` | Demo endpoint that logs incoming alert payloads |

### Examples

```bash
# Latest price (sub-millisecond Redis lookup; curl reports full round-trip time)
curl -w "\nTime: %{time_total}s\n" "http://localhost:8080/api/v1/market/latest?symbol=BTCUSDT"

# Rolling metrics
curl "http://localhost:8080/api/v1/market/metrics?symbol=BTCUSDT"

# Tick history
curl "http://localhost:8080/api/v1/market/history?symbol=BTCUSDT&limit=10"
```

When a configured threshold is breached, the alert engine sends a POST like:

```json
{
  "symbol": "BTCUSDT",
  "priceChangePct": 2.4,
  "windowMinutes": 5,
  "currentPrice": 61234.50,
  "timestamp": 1734000000000
}
```

---

## Project Structure

```
src/main/java/com/market/aggregator/
├── model/            # DTOs (MarketTick, RollingMetric)
├── entity/            # JPA entities (TickEntity, RollingMetricEntity)
├── repository/       # Spring Data repositories
├── service/
│   ├── BinanceWebSocketIngestionService.java
│   ├── KafkaProducerService.java
│   ├── KafkaTickConsumerService.java
│   ├── RollingMetricCalculator.java
│   ├── RedisMarketDataCache.java
│   └── AlertEngineService.java
└── controller/
    ├── MarketDataController.java
    └── WebhookDemoController.java
```

---

## Success Metrics

| Goal | Target |
|---|---|
| Ingestion latency (tick → Kafka publish) | Sub-second |
| Redis read latency (latest price) | Sub-millisecond (measured at the Redis client call) |
| Rolling metric accuracy | Matches manual calculation on raw tick data |
| Alert delivery latency | Under a few seconds from threshold breach |
| Stability | Sustained run without crashes over an extended period |

---

## Out of Scope

- Front-end dashboard / data visualization UI
- Paid/premium market-data APIs
- Multi-tenant or institutional-scale concurrency
- User authentication, authorization, billing
- Full notification system (email, SMS, push) — alerts are delivered via webhook only
- Alert rule CRUD API, alert history audit log, and cooldown/dedup logic
- Multiple simultaneous rolling windows (single configurable window only)

---

## Notes

- If Redis is unreachable, the cache service falls back to an in-memory `ConcurrentHashMap` so the pipeline keeps running.
- Only one rolling window is computed at a time, set via `market.metrics.window-minutes`.
- This is a single-user demo/portfolio system, not designed for concurrent institutional load.
