
package com.citi.marketdata;

import com.citi.marketdata.service.MarketDataPoller;

public class App {

    public static void main(String[] args) {
        MarketDataPoller poller = new MarketDataPoller();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            poller.shutdown();
        }));

        poller.start();
    }
}

