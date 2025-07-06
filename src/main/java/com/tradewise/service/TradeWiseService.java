package com.tradewise.service;

import org.springframework.stereotype.Service;

import com.tradewise.externalservicecaller.AlphaVantageServiceCaller;
import com.tradewise.model.StockData;
import com.tradewise.model.DailyData;
import com.tradewise.repository.StockDataRepository;
import com.tradewise.configs.TradeWiseProps;

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
import java.time.LocalDateTime;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class TradeWiseService {

    private final AlphaVantageServiceCaller alphaVantageServiceCaller;
    private final StockDataRepository stockDataRepository;
    private final DataParserService dataParserService;
    private final TradeWiseProps tradeWiseProps;
    
    public TradeWiseService(AlphaVantageServiceCaller alphaVantageServiceCaller, 
                           StockDataRepository stockDataRepository,
                           DataParserService dataParserService,
                           TradeWiseProps tradeWiseProps) {
        this.alphaVantageServiceCaller = alphaVantageServiceCaller;
        this.stockDataRepository = stockDataRepository;
        this.dataParserService = dataParserService;
        this.tradeWiseProps = tradeWiseProps;
    }

    public Mono<String> getCurrentPrice(String symbol) {
        log.info("Fetching current price for symbol: {}", symbol);
        
        // Check if caching is enabled and data exists in MongoDB
        if (tradeWiseProps.getCache().isEnabled()) {
            return stockDataRepository.findByStockSymbolAndDataType(symbol, "CURRENT")
                    .filter(this::isDataFresh)
                    .doOnNext(data -> log.info("Found fresh current price data in MongoDB for symbol: {}", symbol))
                    .flatMap(this::convertToCurrentPriceResponse)
                    .switchIfEmpty(fetchAndCacheCurrentPrice(symbol));
        } else {
            // Direct API call without caching
            return alphaVantageServiceCaller.getCurrentPrice(symbol);
        }
    }

    public Mono<String> getHistoricalPrice(String symbol) {
        log.info("Fetching historical price for symbol: {}", symbol);
        
        // Check if caching is enabled and data exists in MongoDB
        if (tradeWiseProps.getCache().isEnabled()) {
            return stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL")
                    .filter(this::isDataFresh)
                    .doOnNext(data -> log.info("Found fresh historical data in MongoDB for symbol: {}", symbol))
                    .flatMap(this::analyzeStockDataWithTa4j)
                    .switchIfEmpty(fetchAndCacheHistoricalPrice(symbol));
        } else {
            // Direct API call without caching
            return alphaVantageServiceCaller.getHistoricalPrice(symbol)
                    .flatMap(this::analyzeWithTa4j)
                    .onErrorResume(e -> Mono.just("Error fetching historical price: " + e.getMessage()));
        }
    }
    
    private Mono<String> fetchAndCacheCurrentPrice(String symbol) {
        log.info("Fetching current price from API and caching for symbol: {}", symbol);
        
        return alphaVantageServiceCaller.getCurrentPrice(symbol)
                .flatMap(jsonData -> {
                    try {
                        // Parse and save to MongoDB
                        StockData stockData = dataParserService.parseCurrentPriceData(jsonData, symbol);
                        return stockDataRepository.saveOrUpdate(stockData)
                                .doOnNext(saved -> log.info("Cached current price data for symbol: {}", symbol))
                                .then(convertToCurrentPriceResponse(stockData));
                    } catch (Exception e) {
                        log.error("Error parsing current price data for symbol: {}", symbol, e);
                        return Mono.just(jsonData); // Return original data if parsing fails
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error fetching current price for symbol: {}", symbol, e);
                    return Mono.just("Error fetching current price: " + e.getMessage());
                });
    }
    
    private Mono<String> fetchAndCacheHistoricalPrice(String symbol) {
        log.info("Fetching historical price from API and caching for symbol: {}", symbol);
        
        return alphaVantageServiceCaller.getHistoricalPrice(symbol)
                .flatMap(jsonData -> {
                    try {
                        // Parse and save to MongoDB
                        StockData stockData = dataParserService.parseHistoricalData(jsonData, symbol);
                        return stockDataRepository.saveOrUpdate(stockData)
                                .doOnNext(saved -> log.info("Cached historical data for symbol: {}", symbol))
                                .then(analyzeStockDataWithTa4j(stockData));
                    } catch (Exception e) {
                        log.error("Error parsing historical data for symbol: {}", symbol, e);
                        return analyzeWithTa4j(jsonData); // Fallback to original parsing
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error fetching historical price for symbol: {}", symbol, e);
                    return Mono.just("Error fetching historical price: " + e.getMessage());
                });
    }
    
    private boolean isDataFresh(StockData stockData) {
        if (stockData.getLastUpdated() == null) {
            return false;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(tradeWiseProps.getCache().getTtlMinutes());
        return stockData.getLastUpdated().isAfter(cutoff);

    }
    
    private Mono<String> convertToCurrentPriceResponse(StockData stockData) {
        return Mono.fromCallable(() -> {
            if (stockData.getDailyData().isEmpty()) {
                return "No current price data available";
            }
            
            DailyData latestData = stockData.getDailyData().get(stockData.getDailyData().size() - 1);
            return String.format("Current price for %s: $%.2f (Open: $%.2f, High: $%.2f, Low: $%.2f, Volume: %d)",
                    stockData.getStockSymbol(),
                    latestData.getClosePrice(),
                    latestData.getOpenPrice(),
                    latestData.getHighPrice(),
                    latestData.getLowPrice(),
                    latestData.getVolume());
        });
    }
    
    private Mono<String> analyzeStockDataWithTa4j(StockData stockData) {
        return Mono.fromCallable(() -> {
            if (stockData.getDailyData().isEmpty()) {
                return "No historical data available for analysis";
            }
            
            BarSeries series = new BaseBarSeries("stock_series");
            
            // Convert DailyData to TA4J Bars
            for (DailyData dailyData : stockData.getDailyData()) {
                Bar bar = new BaseBar(
                        Duration.ofDays(1),
                        ZonedDateTime.of(dailyData.getDate().atStartOfDay(), 
                                       java.time.ZoneId.systemDefault()),
                        dailyData.getOpenPrice(),
                        dailyData.getHighPrice(),
                        dailyData.getLowPrice(),
                        dailyData.getClosePrice(),
                        new BigDecimal(dailyData.getVolume())
                );
                series.addBar(bar);
            }
            
            // Calculate indicators
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, 14);
            SMAIndicator sma = new SMAIndicator(closePrice, 14);
            
            double latestRsi = rsi.getValue(series.getEndIndex()).doubleValue();
            double latestSma = sma.getValue(series.getEndIndex()).doubleValue();
            
            log.info("Latest RSI value for {}: {}", stockData.getStockSymbol(), latestRsi);
            log.info("Latest SMA value for {}: {}", stockData.getStockSymbol(), latestSma);
            
            return String.format("Analysis for %s - Latest RSI: %.2f, Latest SMA: %.2f, Data points: %d",
                    stockData.getStockSymbol(), latestRsi, latestSma, series.getBarCount());
        });
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

    /**
     * Performs enhanced technical analysis for MCP server integration
     */
    public Mono<Map<String, Object>> getEnhancedTechnicalAnalysis(String symbol) {
        log.info("Performing enhanced technical analysis for MCP server: {}", symbol);
        
        return stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL")
                .filter(this::isDataFresh)
                .switchIfEmpty(fetchAndCacheHistoricalPrice(symbol).then(
                        stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL")))
                .flatMap(stockData -> {
                    return Mono.fromCallable(() -> {
                        if (stockData.getDailyData().isEmpty()) {
                            Map<String, Object> errorResult = new HashMap<>();
                            errorResult.put("error", "No historical data available");
                            return errorResult;
                        }
                        
                        BarSeries series = new BaseBarSeries("mcp_analysis");
                        
                        // Convert to TA4J format
                        for (DailyData dailyData : stockData.getDailyData()) {
                            Bar bar = new BaseBar(
                                    Duration.ofDays(1),
                                    ZonedDateTime.of(dailyData.getDate().atStartOfDay(), 
                                                   java.time.ZoneId.systemDefault()),
                                    dailyData.getOpenPrice(),
                                    dailyData.getHighPrice(),
                                    dailyData.getLowPrice(),
                                    dailyData.getClosePrice(),
                                    new BigDecimal(dailyData.getVolume())
                            );
                            series.addBar(bar);
                        }
                        
                        return calculateAllTechnicalIndicators(series, stockData.getStockSymbol());
                    });
                })
                .onErrorResume(e -> {
                    log.error("Error in enhanced technical analysis for symbol: {}", symbol, e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "Analysis failed: " + e.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * Calculates comprehensive technical indicators for MCP server
     */
    private Map<String, Object> calculateAllTechnicalIndicators(BarSeries series, String symbol) {
        Map<String, Object> indicators = new HashMap<>();
        
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            
            // RSI
            RSIIndicator rsi = new RSIIndicator(closePrice, 14);
            indicators.put("rsi", rsi.getValue(series.getEndIndex()).doubleValue());
            
            // Simple Moving Averages
            SMAIndicator sma14 = new SMAIndicator(closePrice, 14);
            SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
            indicators.put("sma14", sma14.getValue(series.getEndIndex()).doubleValue());
            if (series.getBarCount() >= 50) {
                indicators.put("sma50", sma50.getValue(series.getEndIndex()).doubleValue());
            }
            
            // Current price info
            Bar lastBar = series.getLastBar();
            indicators.put("currentPrice", lastBar.getClosePrice().doubleValue());
            indicators.put("volume", lastBar.getVolume().longValue());
            indicators.put("high", lastBar.getHighPrice().doubleValue());
            indicators.put("low", lastBar.getLowPrice().doubleValue());
            indicators.put("open", lastBar.getOpenPrice().doubleValue());
            
            // Analysis metadata
            indicators.put("symbol", symbol);
            indicators.put("dataPoints", series.getBarCount());
            indicators.put("lastUpdated", LocalDateTime.now().toString());
            
            // Trading signals
            double rsiValue = rsi.getValue(series.getEndIndex()).doubleValue();
            indicators.put("signal", generateTradingSignal(rsiValue));
            indicators.put("trend", determineTrend(rsiValue, 
                    sma14.getValue(series.getEndIndex()).doubleValue(),
                    lastBar.getClosePrice().doubleValue()));
            
        } catch (Exception e) {
            log.error("Error calculating technical indicators", e);
            indicators.put("error", "Calculation failed: " + e.getMessage());
        }
        
        return indicators;
    }
    
    private String generateTradingSignal(double rsi) {
        if (rsi > 70) {
            return "SELL - RSI indicates overbought conditions";
        } else if (rsi < 30) {
            return "BUY - RSI indicates oversold conditions";
        } else if (rsi > 50) {
            return "HOLD/WEAK_BUY - RSI indicates slight bullish momentum";
        } else {
            return "HOLD/WEAK_SELL - RSI indicates slight bearish momentum";
        }
    }
    
    private String determineTrend(double rsi, double sma, double currentPrice) {
        if (rsi > 70) {
            return "OVERBOUGHT";
        } else if (rsi < 30) {
            return "OVERSOLD";
        } else if (currentPrice > sma * 1.02) {
            return "STRONG_BULLISH";
        } else if (currentPrice > sma) {
            return "BULLISH";
        } else if (currentPrice < sma * 0.98) {
            return "STRONG_BEARISH";
        } else if (currentPrice < sma) {
            return "BEARISH";
        } else {
            return "NEUTRAL";
        }
    }
}
