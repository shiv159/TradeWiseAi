package com.tradewise.mcp.service;

import com.tradewise.mcp.dto.TechnicalAnalysisResult;
import com.tradewise.service.TradeWiseService;
import com.tradewise.model.StockData;
import com.tradewise.model.DailyData;
import com.tradewise.repository.StockDataRepository;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // Structured technical analysis result for MCP
    public Mono<TechnicalAnalysisResult> performTechnicalAnalysis(String symbol) {
        log.info("Performing technical analysis for symbol: {}", symbol);
        return tradeWiseService.getEnhancedTechnicalAnalysis(symbol)
                .map(indicators -> TechnicalAnalysisResult.builder()
                        .symbol(symbol)
                        .currentPrice(asDouble(indicators.get("currentPrice")))
                        .rsi(asDouble(indicators.get("rsi")))
                        .sma14(asDouble(indicators.get("sma14")))
                        .trend(asString(indicators.get("trend")))
                        .signal(asString(indicators.get("signal")))
                        .dataPoints(asInt(indicators.get("dataPoints")))
                        .lastUpdated(asString(indicators.get("lastUpdated")))
                        .open(asDouble(indicators.get("open")))
                        .high(asDouble(indicators.get("high")))
                        .low(asDouble(indicators.get("low")))
                        .volume(asLong(indicators.get("volume")))
                        .build())
                .timeout(Duration.ofSeconds(30));
    }

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
                .timeout(Duration.ofSeconds(30));
    }

    public Mono<String> performAdvancedAnalysis(String symbol, int days) {
        log.info("Performing advanced analysis for symbol: {} for {} days", symbol, days);
        return Mono.just(String.format("Advanced analysis for %s (%d days): [Details omitted]", symbol, days));
    }

    public Mono<String> getCurrentPriceFormatted(String symbol) {
        return tradeWiseService.getCurrentPrice(symbol)
                .map(price -> String.format("Current price for %s: %s", symbol, price))
                .timeout(Duration.ofSeconds(30));
    }

    public Mono<String> searchStocksFormatted(String query) {
        return Mono.fromCallable(() -> searchStocks(query)).map(results -> {
            StringBuilder content = new StringBuilder("Search results for '" + query + "':\n");
            for (Map<String, Object> result : results) {
                content.append(String.format("- %s (%s): %s [%s]\n",
                        result.get("symbol"), result.get("market"),
                        result.get("name"), result.get("status")));
            }
            return content.toString();
        }).timeout(Duration.ofSeconds(15));
    }

    private Mono<StockData> fetchHistoricalData(String symbol) {
        return tradeWiseService.getHistoricalPrice(symbol)
                .then(stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL"));
    }

    private java.util.List<java.util.Map<String, Object>> searchStocks(String query) {
    log.info("Searching for stocks with query: {}", query);
    return java.util.List.of(
        java.util.Map.of(
            "symbol", query.toUpperCase(),
            "name", "Stock: " + query.toUpperCase(),
            "market", "BSE",
            "status", "active"
        )
    );
    }

    private Double asDouble(Object o) { return o == null ? null : ((Number) o).doubleValue(); }
    private Long asLong(Object o) { return o == null ? null : ((Number) o).longValue(); }
    private Integer asInt(Object o) { return o == null ? null : ((Number) o).intValue(); }
    private String asString(Object o) { return o == null ? null : String.valueOf(o); }
}
