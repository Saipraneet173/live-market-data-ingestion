package com.citi.marketdata.service;

import com.citi.marketdata.model.TickData;
import yahoofinance.YahooFinance;
import yahoofinance.Stock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketDataPoller {
    private static final String SYMBOL = "^DJI";

    private final Queue<TickData> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void start(){
        scheduler.scheduleAtFixedRate(this::poll, 0, 5, TimeUnit.SECONDS);
        System.out.println("Poller started. Fetching "+ SYMBOL + " every 5 seconds...");
    }

    private void poll(){
        try{
            Stock stock = YahooFinance.get(SYMBOL);
            BigDecimal price = stock.getQuote().getPrice();
            Instant timestamp = Instant.now();

            TickData tick = new TickData(timestamp, SYMBOL, price);
            queue.offer(tick);

            System.out.println("Enqueued: " + tick + " | Queue size: " + queue.size());
        } catch (Exception e){
            System.err.println("Poll failed: " + e.getMessage());
        }
    }

    public void shutdown(){
        System.out.println("shutting down poller...");
        scheduler.shutdown();
    }

    public Queue<TickData> getQueue() {
        return queue;
    }

    
}
