package com.citi.marketdata.model;

import java.math.BigDecimal;
import java.time.Instant;

public class TickData {
    private final Instant timestamp;
    private final String symbol;
    private final BigDecimal price;

    public TickData(Instant timestamp, String symbol, BigDecimal price) {
        this.timestamp = timestamp;
        this.symbol = symbol;
        this.price = price;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public String toString(){
        return "[" + timestamp + "] " + symbol + " @ " + price;
    }
}
