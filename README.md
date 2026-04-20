# Live Market Data Ingestion Service (DJIA)

A lightweight, production-ready Java application that continuously polls live stock pricing for the **Dow Jones Industrial Average (^DJI)** at a strict 5-second interval, stores each reading in a thread-safe in-memory queue, and displays a **live-updating JavaFX line chart** for real-time visualisation.

Built as part of the **Citi Software Engineering Forage Program**.

---

## Tech Stack

- **Language:** Java 21
- **Build Tool:** Gradle (Groovy DSL)
- **API:** Yahoo Finance (`com.yahoofinance-api:YahooFinanceAPI:3.17.0`)
- **UI:** JavaFX 21 (`javafx.controls`)
- **Testing:** JUnit Jupiter

---

## Project Structure

```
app/src/main/java/com/citi/marketdata/
├── App.java                    # JavaFX entry point — builds and drives the live chart
├── model/
│   └── TickData.java           # Data model: timestamp, symbol, price
└── service/
    └── MarketDataPoller.java   # Scheduler, API client, queue manager, and tick callback
```

---

## How to Run

> **Prerequisites:** Java 21+ installed. No local Gradle installation needed — use the included `./gradlew` wrapper.

```bash
# Clone the repo
git clone https://github.com/Saipraneet173/live-market-data-ingestion.git
cd live-market-data-ingestion

# Run the application (opens the live chart window)
./gradlew run

# Run unit tests
./gradlew test

# Build everything
./gradlew build
```

**To stop:** close the chart window or press `Ctrl+C` — the poller shuts down cleanly either way.

---

## How It Works

```
App.java (JavaFX Application)
  └── start(Stage)
        ├── Builds LineChart (CategoryAxis × NumberAxis)
        ├── Plots any existing queue data immediately
        ├── Registers onTick callback → Platform.runLater() → chart update
        └── Starts MarketDataPoller (background thread)
              └── authenticate()        — gets session cookie + crumb from Yahoo Finance
              └── scheduleAtFixedRate() — fires poll() every 5 seconds
                    └── poll()
                          ├── Calls Yahoo Finance API
                          ├── Extracts BigDecimal price + Instant timestamp
                          ├── Wraps in TickData → ConcurrentLinkedQueue
                          └── Fires onTick callback → new point appears on chart
```

**Sample console output:**
```
Poller started. Fetching ^DJI every 5 seconds...
Enqueued: [2026-04-20T10:10:13Z] ^DJI @ 42103.45 | Queue size: 1
Enqueued: [2026-04-20T10:10:18Z] ^DJI @ 42098.12 | Queue size: 2
Enqueued: [2026-04-20T10:10:23Z] ^DJI @ 42115.67 | Queue size: 3
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
| Graceful shutdown | JavaFX `stop()` override | Cleans up threads safely on window close or `Ctrl+C` |
| UI thread safety | `Platform.runLater()` | JavaFX requires all chart updates on its own thread |
| Chart scaling | `yAxis.setForceZeroInRange(false)` | Auto-scales around actual price range, making variations visible |

---

## Data Model

Each item stored in the queue is a `TickData` object:

```java
TickData {
    Instant    timestamp;  // UTC time of capture e.g. 2026-04-20T10:10:13Z
    String     symbol;     // Always "^DJI"
    BigDecimal price;      // e.g. 42103.45
}
```

---

## Notes

Yahoo Finance's unofficial API has increasingly restricted programmatic access. If the live API is unavailable (HTTP 429/401), the application falls back to simulated prices tagged `[SIMULATED]` in the console output, so the full scheduling, queuing, and charting pipeline is always demonstrable.
