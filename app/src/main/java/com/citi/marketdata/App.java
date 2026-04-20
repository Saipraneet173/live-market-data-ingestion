package com.citi.marketdata;

import com.citi.marketdata.model.TickData;
import com.citi.marketdata.service.MarketDataPoller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class App extends Application {
    private final MarketDataPoller poller = new MarketDataPoller();
    private XYChart.Series<String, Number> series;

    @Override
    public void start(Stage stage){
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("DJIA price ($)");
        yAxis.setForceZeroInRange(false);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Live DJIA Price");
        chart.setAnimated(false);

        series = new XYChart.Series<>();
        series.setName("^DJI");
        chart.getData().add(series);

        for (TickData tick : poller.getQueue()) {
            addToChart(tick);
        }

        poller.setOnTick(tick -> Platform.runLater(() -> addToChart(tick)));
        poller.start();

        Scene scene = new Scene(chart, 900, 600);
        stage.setTitle("DJIA Live Market Data");
        stage.setScene(scene);
        stage.show();
    }

    private void addToChart(TickData tick){
        String time = tick.getTimestamp().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        series.getData().add(new XYChart.Data<>(time, tick.getPrice()));
    }

    @Override
    public void stop() {
        poller.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }

}

