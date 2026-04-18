# Live Market Data Ingestion Service (DJIA)

A lightweight, production-ready Java application that continuously polls live stock pricing for the **Dow Jones Industrial Average (^DJI)** at a strict 5-second interval and stores each reading in a thread-safe in-memory queue for downstream processing.

Built as part of the **Citi Software Engineering Forage Program**.

---

## Tech Stack

- **Language:** Java 21
- **Build Tool:** Gradle (Groovy DSL)
- **API:** Yahoo Finance (`com.yahoofinance-api:YahooFinanceAPI:3.17.0`)
- **Testing:** JUnit Jupiter

---

## Project Structure

```
app/src/main/java/com/citi/marketdata/
├── App.java                    # Entry point + JVM shutdown hook
├── model/
│   └── TickData.java           # Data model: timestamp, symbol, price
└── service/
    └── MarketDataPoller.java   # Scheduler, API client, and queue manager
```

---

## How to Run

> **Prerequisites:** Java 21+ and Gradle installed, or just use the included Gradle wrapper (`./gradlew`) which requires no local Gradle installation.

```bash
# Clone the repo
git clone https://github.com/Saipraneet173/live-market-data-ingestion.git
cd live-market-data-ingestion

# Run the application
./gradlew run

# Run unit tests
./gradlew test

# Build everything
./gradlew build
```

**To stop:** press `Ctrl+C` — the shutdown hook fires automatically and closes the scheduler cleanly.

---

## How It Works

```
App.java
  └── Registers shutdown hook
  └── Starts MarketDataPoller
        └── authenticate()        — gets session cookie + crumb from Yahoo Finance
        └── scheduleAtFixedRate() — fires poll() every 5 seconds
              └── poll()
                    ├── Calls Yahoo Finance API
                    ├── Extracts BigDecimal price + Instant timestamp
                    ├── Wraps in TickData
                    └── Offers to ConcurrentLinkedQueue
```

**Sample output:**
```
Poller started. Fetching ^DJI every 5 seconds...
Enqueued: [2026-04-18T12:37:15Z] ^DJI @ 42103.45 | Queue size: 1
Enqueued: [2026-04-18T12:37:20Z] ^DJI @ 42098.12 | Queue size: 2
Enqueued: [2026-04-18T12:37:25Z] ^DJI @ 42115.67 | Queue size: 3
```

---

## Key Engineering Decisions

| Requirement | Implementation | Reason |
|---|---|---|
| Strict 5-second cadence | `ScheduledExecutorService.scheduleAtFixedRate` | No drift — unlike `Thread.sleep` which accumulates delay |
| Thread-safe queue | `ConcurrentLinkedQueue<TickData>` | Safe for concurrent reads and writes without crashing |
| Exact financial precision | `BigDecimal` for price | `double` cannot represent most decimals exactly |
| Unambiguous timestamps | `Instant` (UTC) | No timezone dependency |
| Fault tolerance | `try-catch` wrapping each poll | One failed API call never stops the scheduler |
| Graceful shutdown | JVM shutdown hook | Cleans up threads safely on `Ctrl+C` or `SIGTERM` |

---

## Data Model

Each item stored in the queue is a `TickData` object:

```java
TickData {
    Instant   timestamp;  // UTC time of capture e.g. 2026-04-18T12:37:15Z
    String    symbol;     // Always "^DJI"
    BigDecimal price;     // e.g. 42103.45
}
```

---

## Notes

This application implements the **producer side only** — it populates the queue but does not process or consume from it. No GUI, no database, no consumer logic is in scope.

Yahoo Finance's unofficial API has increasingly restricted programmatic access. If the live API is unavailable (HTTP 429/401), the application falls back to simulated prices tagged `[SIMULATED]` in the output, so the full scheduling and queuing pipeline is always demonstrable.
