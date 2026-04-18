package com.citi.marketdata.service;

import com.citi.marketdata.model.TickData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MarketDataPoller {

    private static final String SYMBOL = "^DJI";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final BigDecimal BASE_PRICE = new BigDecimal("42000.00");
    private final Random random = new Random();

    private final Queue<TickData> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private String sessionCookie;
    private String crumb;

    public void start() {
        try {
            authenticate();
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
        }
        scheduler.scheduleAtFixedRate(this::poll, 0, 5, TimeUnit.SECONDS);
        System.out.println("Poller started. Fetching " + SYMBOL + " every 5 seconds...");
    }

    private void authenticate() throws IOException {
        // Step 1: visit Yahoo Finance homepage to obtain a session cookie
        HttpURLConnection conn = (HttpURLConnection)
                URI.create("https://finance.yahoo.com/").toURL().openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setInstanceFollowRedirects(true);
        conn.connect();

        Map<String, List<String>> headers = conn.getHeaderFields();
        List<String> cookieHeaders = headers.getOrDefault("Set-Cookie", List.of());
        sessionCookie = cookieHeaders.stream()
                .map(c -> c.split(";")[0])
                .collect(Collectors.joining("; "));
        conn.disconnect();

        // Step 2: exchange the session cookie for a crumb token
        HttpURLConnection crumbConn = (HttpURLConnection)
                URI.create("https://query1.finance.yahoo.com/v1/test/getcrumb").toURL().openConnection();
        crumbConn.setRequestProperty("User-Agent", USER_AGENT);
        crumbConn.setRequestProperty("Cookie", sessionCookie);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(crumbConn.getInputStream()))) {
            crumb = reader.lines().collect(Collectors.joining()).trim();
        }
        System.out.println("Authenticated. Crumb obtained.");
    }

    private void poll() {
        try {
            String url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=%5EDJI&crumb=" + crumb;
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Cookie", sessionCookie);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining());
                BigDecimal price = extractPrice(response);
                enqueue(price, false);
            }
        } catch (Exception e) {
            System.err.println("API unavailable (" + e.getMessage() + ") — using simulated price.");
            enqueue(simulatePrice(), true);
        }
    }

    private void enqueue(BigDecimal price, boolean simulated) {
        Instant timestamp = Instant.now();
        TickData tick = new TickData(timestamp, SYMBOL, price);
        queue.offer(tick);
        String tag = simulated ? " [SIMULATED]" : "";
        System.out.println("Enqueued" + tag + ": " + tick + " | Queue size: " + queue.size());
    }

    private BigDecimal simulatePrice() {
        double variation = (random.nextDouble() - 0.5) * 200;
        return BASE_PRICE.add(new BigDecimal(variation).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private BigDecimal extractPrice(String json) {
        String marker = "\"regularMarketPrice\":";
        int idx = json.indexOf(marker);
        String after = json.substring(idx + marker.length());
        String priceStr = after.split("[,}]")[0].trim();
        return new BigDecimal(priceStr);
    }

    public void shutdown() {
        System.out.println("Shutting down poller...");
        scheduler.shutdown();
    }

    public Queue<TickData> getQueue() {
        return queue;
    }
}
