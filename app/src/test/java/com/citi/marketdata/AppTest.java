package com.citi.marketdata;

import com.citi.marketdata.model.TickData;
import com.citi.marketdata.service.MarketDataPoller;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @Test
    void queueIsInitiallyEmpty() {
        MarketDataPoller poller = new MarketDataPoller();
        assertTrue(poller.getQueue().isEmpty());
    }

    @Test
    void tickDataStoresValuesCorrectly() {
        Instant now = Instant.now();
        BigDecimal price = new BigDecimal("39847.23");
        TickData tick = new TickData(now, "^DJI", price);

        assertEquals(now, tick.getTimestamp());
        assertEquals("^DJI", tick.getSymbol());
        assertEquals(price, tick.getPrice());
    }

    @Test
    void queueMaintainsFifoOrder() {
        MarketDataPoller poller = new MarketDataPoller();

        TickData first  = new TickData(Instant.now(), "^DJI", new BigDecimal("100.00"));
        TickData second = new TickData(Instant.now(), "^DJI", new BigDecimal("200.00"));

        poller.getQueue().offer(first);
        poller.getQueue().offer(second);

        assertEquals(first,  poller.getQueue().poll());
        assertEquals(second, poller.getQueue().poll());
    }

    @Test
    void shutdownDoesNotThrow() {
        MarketDataPoller poller = new MarketDataPoller();
        assertDoesNotThrow(() -> poller.shutdown());
    }
}
