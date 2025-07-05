package com.tradewise.service;

import org.springframework.stereotype.Service;

import com.tradewise.externalservicecaller.AlphaVantageServiceCaller;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZonedDateTime;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class TradeWiseService {

    private final AlphaVantageServiceCaller alphaVantageServiceCaller;
    public TradeWiseService(AlphaVantageServiceCaller alphaVantageServiceCaller) {
        this.alphaVantageServiceCaller = alphaVantageServiceCaller;
    }

    public Mono<String> getCurrentPrice(String symbol) {
       return alphaVantageServiceCaller.getCurrentPrice(symbol);        
    }

    public Mono<String> getHistoricalPrice(String symbol) {
        return alphaVantageServiceCaller.getHistoricalPrice(symbol)
            .flatMap(historicalJson -> analyzeWithTa4j(historicalJson))
            .onErrorResume(e -> Mono.just("Error fetching historical price: " + e.getMessage()));
    }

    /**
     * Deeply analyzes stock data using TA4J indicators (e.g., RSI).
     * Only uses historical price data for calculation.
     * @param historicalJson JSON string of historical prices
     * @return Mono<String> with analysis result
     */
    public Mono<String> analyzeWithTa4j(String historicalJson) {
        return Mono.fromCallable(() -> {
            ObjectMapper mapper = new ObjectMapper();
            BarSeries series = new BaseBarSeries("stock_series");

            // Parse historical data
            JsonNode historicalRoot = mapper.readTree(historicalJson);
            JsonNode timeSeries = historicalRoot.path("Time Series (Daily)");
            // Collect and sort dates in ascending order
            List<String> dateList = new ArrayList<>();
            timeSeries.fieldNames().forEachRemaining(dateList::add);
            dateList.sort(Comparator.naturalOrder());
            for (String dateStr : dateList) {
                JsonNode barNode = timeSeries.get(dateStr);
                Bar bar = new BaseBar(
                    Duration.ofDays(1),
                    ZonedDateTime.parse(dateStr + "T00:00:00Z"),
                    new BigDecimal(barNode.get("1. open").asText()),
                    new BigDecimal(barNode.get("2. high").asText()),
                    new BigDecimal(barNode.get("3. low").asText()),
                    new BigDecimal(barNode.get("4. close").asText()),
                    new BigDecimal(barNode.get("5. volume").asText())
                );
                series.addBar(bar);
            }

            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, 14);
            SMAIndicator sma = new SMAIndicator(closePrice, 14);
            double latestRsi = rsi.getValue(series.getEndIndex()).doubleValue();
            double latestSma = sma.getValue(series.getEndIndex()).doubleValue();
            log.info("Latest RSI value: {}", latestRsi);
            log.info("Latest SMA value: {}", latestSma);
            return "Latest RSI: " + latestRsi + ", Latest SMA: " + latestSma;
        });
    }

}
