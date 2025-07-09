package com.tradewise.mcp.service;

import com.tradewise.service.TradeWiseService;
import com.tradewise.model.StockData;
import com.tradewise.model.DailyData;
import com.tradewise.repository.StockDataRepository;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@Slf4j
public class McpAnalysisService {
    
    private final TradeWiseService tradeWiseService;
    private final StockDataRepository stockDataRepository;
    
    public McpAnalysisService(TradeWiseService tradeWiseService, 
                             StockDataRepository stockDataRepository) {
        this.tradeWiseService = tradeWiseService;
        this.stockDataRepository = stockDataRepository;
    }
    
    /**
     * Performs comprehensive technical analysis for MCP clients
     */
    public Mono<String> performTechnicalAnalysis(String symbol) {
        log.info("Performing technical analysis for symbol: {}", symbol);
        return tradeWiseService.getEnhancedTechnicalAnalysis(symbol)
                .map(indicators -> {
                    if (indicators.containsKey("error")) {
                        return "ERROR: " + indicators.get("error");
                    }
                    return String.format(
                        "Technical Analysis for %s:\n\nCurrent Price: $%.2f\nRSI (14): %.2f\nSMA (14): %.2f\nTrend: %s\nSignal: %s\n",
                        symbol,
                        indicators.getOrDefault("currentPrice", 0.0),
                        indicators.getOrDefault("rsi", 0.0),
                        indicators.getOrDefault("sma14", 0.0),
                        indicators.getOrDefault("trend", "UNKNOWN"),
                        indicators.getOrDefault("signal", "No signal")
                    );
                })
                .onErrorResume(e -> {
                    log.error("Error performing technical analysis for symbol: {}", symbol, e);
                    return Mono.just("ERROR: " + e.getMessage());
                });
    }
    
    /**
     * Retrieves historical data with technical indicators for MCP clients
     */
    public Mono<String> getHistoricalAnalysis(String symbol, int days) {
        log.info("Getting historical analysis for symbol: {} for {} days", symbol, days);
        return stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL")
                .switchIfEmpty(fetchHistoricalData(symbol))
                .map(stockData -> {
                    List<DailyData> limitedData = stockData.getDailyData().stream()
                            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                            .limit(days)
                            .collect(Collectors.toList());
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("Historical Analysis for %s (%d days):\n\n", symbol, days));
                    if (!limitedData.isEmpty()) {
                        sb.append("Recent Price Data:\n");
                        limitedData.stream().limit(5).forEach(data -> sb.append(
                            String.format("- %s: O:%.2f H:%.2f L:%.2f C:%.2f V:%d\n",
                                data.getDate(), data.getOpenPrice().doubleValue(), data.getHighPrice().doubleValue(),
                                data.getLowPrice().doubleValue(), data.getClosePrice().doubleValue(), data.getVolume())
                        ));
                    }
                    return sb.toString();
                })
                .onErrorResume(e -> {
                    log.error("Error getting historical analysis for symbol: {}", symbol, e);
                    return Mono.just("ERROR: " + e.getMessage());
                });
    }
    
    /**
     * Performs advanced pattern recognition and market sentiment analysis
     */
    public Mono<String> performAdvancedAnalysis(String symbol, int days) {
        log.info("Performing advanced analysis for symbol: {} for {} days", symbol, days);
        // For demonstration, just return a placeholder string
        return Mono.just(String.format("Advanced analysis for %s (%d days): [Details omitted]", symbol, days));
    }
    
    /**
     * Gets current stock price and basic information
     */
    public Mono<String> getCurrentPriceFormatted(String symbol) {
        return tradeWiseService.getCurrentPrice(symbol)
            .map(price -> String.format("Current price for %s: %s", symbol, price))
            .onErrorResume(e -> Mono.just("ERROR: " + e.getMessage()));
    }
    
    /**
     * Searches for stock symbols based on query
     */
    public Mono<String> searchStocksFormatted(String query) {
        return Mono.just(searchStocks(query)).map(results -> {
            StringBuilder content = new StringBuilder("Search results for '" + query + "':\n");
            for (Map<String, Object> result : results) {
                content.append(String.format("- %s (%s): %s [%s]\n",
                        result.get("symbol"), result.get("market"),
                        result.get("name"), result.get("status")));
            }
            return content.toString();
        });
    }
    
    private Mono<StockData> fetchHistoricalData(String symbol) {
        return tradeWiseService.getHistoricalPrice(symbol)
                .then(stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL"));
    }
    
    private List<Map<String, Object>> searchStocks(String query) {
        log.info("Searching for stocks with query: {}", query);
        
        // For now, return a simple response. In a real implementation, 
        // you might integrate with a stock search API
        return List.of(
                Map.of(
                        "symbol", query.toUpperCase(),
                        "name", "Stock: " + query.toUpperCase(),
                        "market", "BSE",
                        "status", "active"
                )
        );
    }
}
