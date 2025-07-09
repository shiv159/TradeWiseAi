package com.tradewise.mcp.controller;

import com.tradewise.mcp.model.McpResponse;
import com.tradewise.mcp.service.McpAnalysisService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class McpTools {
    private final McpAnalysisService mcpAnalysisService;

    public McpTools(McpAnalysisService mcpAnalysisService) {
        this.mcpAnalysisService = mcpAnalysisService;
    }

    @Tool(name = "technical_analysis", description = "Performs comprehensive technical analysis on a stock symbol")
    public McpResponse.ToolResult technicalAnalysis(
            @ToolParam(description = "Stock symbol to analyze") String symbol) {
        var result = mcpAnalysisService.performTechnicalAnalysis(symbol).block();
        return McpResponse.ToolResult.builder()
                .content(formatTechnicalAnalysisResult(result))
                .isError(false)
                .meta(Map.of("symbol", symbol, "type", "technical_analysis"))
                .build();
    }

    @Tool(name = "historical_analysis", description = "Retrieves historical stock data with technical indicators")
    public McpResponse.ToolResult historicalAnalysis(
            @ToolParam(description = "Stock symbol to analyze") String symbol,
            @ToolParam(description = "Number of days of historical data to retrieve") Integer days) {
        var result = mcpAnalysisService.getHistoricalAnalysis(symbol, days).block();
        return McpResponse.ToolResult.builder()
                .content(formatHistoricalAnalysisResult(result))
                .isError(false)
                .meta(Map.of("symbol", symbol, "days", days, "type", "historical_analysis"))
                .build();
    }

    @Tool(name = "advanced_analysis", description = "Performs advanced pattern recognition and market sentiment analysis")
    public McpResponse.ToolResult advancedAnalysis(
            @ToolParam(description = "Stock symbol to analyze") String symbol,
            @ToolParam(description = "Number of days for analysis") Integer days) {
        var result = mcpAnalysisService.performAdvancedAnalysis(symbol, days).block();
        return McpResponse.ToolResult.builder()
                .content(formatAdvancedAnalysisResult(result))
                .isError(false)
                .meta(Map.of("symbol", symbol, "days", days, "type", "advanced_analysis"))
                .build();
    }

    @Tool(name = "current_price", description = "Gets current stock price and basic information")
    public McpResponse.ToolResult currentPrice(
            @ToolParam(description = "Stock symbol to get current price for") String symbol) {
        var result = mcpAnalysisService.performTechnicalAnalysis(symbol).block();
        String content = String.format("Current price for %s: $%.2f", symbol, result.getCurrentPrice());
        return McpResponse.ToolResult.builder()
                .content(content)
                .isError(false)
                .meta(Map.of("symbol", symbol, "price", result.getCurrentPrice(), "type", "current_price"))
                .build();
    }

    @Tool(name = "search_stocks", description = "Searches for stock symbols based on query")
    public McpResponse.ToolResult searchStocks(
            @ToolParam(description = "Search query for stock symbols") String query) {
        List<Map<String, Object>> results = mcpAnalysisService.searchStocks(query).block();
        StringBuilder content = new StringBuilder("Search results for '" + query + "':\n");
        for (Map<String, Object> result : results) {
            content.append(String.format("- %s (%s): %s [%s]\n",
                    result.get("symbol"), result.get("market"),
                    result.get("name"), result.get("status")));
        }
        return McpResponse.ToolResult.builder()
                .content(content.toString())
                .isError(false)
                .meta(Map.of("query", query, "results", results.size(), "type", "search_stocks"))
                .build();
    }

    // Formatting methods (copied from the old controller)
    private String formatTechnicalAnalysisResult(McpResponse.TechnicalAnalysisResult result) {
        return String.format("""
                Technical Analysis for %s:
                
                Current Price: $%.2f
                RSI (14): %.2f
                SMA (14): %.2f
                Trend: %s
                Signal: %s
                
                Analysis Date: %s
                Data Points: %d
                """,
                result.getSymbol(),
                result.getCurrentPrice(),
                result.getRsi(),
                result.getSma(),
                result.getTrend(),
                result.getSignal(),
                result.getAnalysisDate(),
                result.getDataPoints());
    }

    private String formatHistoricalAnalysisResult(McpResponse.HistoricalAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Historical Analysis for %s (%s):\n\n",
                result.getSymbol(), result.getPeriod()));
        sb.append("Technical Indicators:\n");
        McpResponse.TechnicalIndicators indicators = result.getIndicators();
        sb.append(String.format("- RSI: %.2f\n", indicators.getRsi()));
        sb.append(String.format("- SMA (14): %.2f\n", indicators.getSma14()));
        sb.append(String.format("- SMA (50): %.2f\n", indicators.getSma50()));
        sb.append(String.format("- EMA (12): %.2f\n", indicators.getEma12()));
        sb.append(String.format("- EMA (26): %.2f\n", indicators.getEma26()));
        sb.append(String.format("- MACD: %.2f\n", indicators.getMacd()));
        sb.append(String.format("\nTotal Data Points: %d\n", result.getTotalDataPoints()));
        if (!result.getHistoricalData().isEmpty()) {
            sb.append("\nRecent Price Data:\n");
            result.getHistoricalData().stream()
                    .limit(5)
                    .forEach(data -> sb.append(String.format("- %s: O:%.2f H:%.2f L:%.2f C:%.2f V:%d\n",
                            data.getDate(), data.getOpen(), data.getHigh(),
                            data.getLow(), data.getClose(), data.getVolume())));
        }
        return sb.toString();
    }

    private String formatAdvancedAnalysisResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(String.format("Advanced Analysis for %s:\n\n", result.get("symbol")));
            sb.append(String.format("Analysis Period: %s\n", result.get("period")));
            sb.append(String.format("Data Points: %s\n", result.get("dataPoints")));
            sb.append(String.format("Timestamp: %s\n\n", result.get("analysisTimestamp")));
            if (result.containsKey("pricePatterns")) {
                sb.append("=== PRICE PATTERNS ===\n");
                Map<String, Object> pricePatterns = (Map<String, Object>) result.get("pricePatterns");
                sb.append(String.format("MACD Trend: %s\n", pricePatterns.get("macdTrend")));
                sb.append(String.format("Momentum: %s\n", pricePatterns.get("momentum")));
                sb.append(String.format("Volatility: %s\n", pricePatterns.get("volatility")));
                if (pricePatterns.containsKey("gaps")) {
                    List<String> gaps = (List<String>) pricePatterns.get("gaps");
                    if (!gaps.isEmpty()) {
                        sb.append("Recent Gaps:\n");
                        gaps.forEach(gap -> sb.append(String.format("- %s\n", gap)));
                    }
                }
                sb.append("\n");
            }
            if (result.containsKey("volumePatterns")) {
                sb.append("=== VOLUME PATTERNS ===\n");
                Map<String, Object> volumePatterns = (Map<String, Object>) result.get("volumePatterns");
                sb.append(String.format("Volume Trend: %s\n", volumePatterns.get("volumeTrend")));
                sb.append(String.format("Volume-Price Relation: %s\n", volumePatterns.get("volumePriceRelation")));
                sb.append("\n");
            }
            if (result.containsKey("candlestickPatterns")) {
                sb.append("=== CANDLESTICK PATTERNS ===\n");
                Map<String, Object> candlestickPatterns = (Map<String, Object>) result.get("candlestickPatterns");
                List<String> dojiPatterns = (List<String>) candlestickPatterns.get("dojiPatterns");
                if (dojiPatterns != null && !dojiPatterns.isEmpty()) {
                    sb.append("Doji Patterns:\n");
                    dojiPatterns.forEach(pattern -> sb.append(String.format("- %s\n", pattern)));
                }
                List<String> hammerPatterns = (List<String>) candlestickPatterns.get("hammerPatterns");
                if (hammerPatterns != null && !hammerPatterns.isEmpty()) {
                    sb.append("Hammer Patterns:\n");
                    hammerPatterns.forEach(pattern -> sb.append(String.format("- %s\n", pattern)));
                }
                List<String> engulfingPatterns = (List<String>) candlestickPatterns.get("engulfingPatterns");
                if (engulfingPatterns != null && !engulfingPatterns.isEmpty()) {
                    sb.append("Engulfing Patterns:\n");
                    engulfingPatterns.forEach(pattern -> sb.append(String.format("- %s\n", pattern)));
                }
                sb.append("\n");
            }
            if (result.containsKey("trendAnalysis")) {
                sb.append("=== TREND ANALYSIS ===\n");
                Map<String, Object> trendAnalysis = (Map<String, Object>) result.get("trendAnalysis");
                sb.append(String.format("ADX: %.2f\n", trendAnalysis.get("adx")));
                sb.append(String.format("Trend Strength: %s\n", trendAnalysis.get("trendStrength")));
                if (trendAnalysis.containsKey("maSlopes")) {
                    sb.append("Moving Average Slopes:\n");
                    Map<String, String> maSlopes = (Map<String, String>) trendAnalysis.get("maSlopes");
                    maSlopes.forEach((key, value) -> sb.append(String.format("- %s: %s\n", key, value)));
                }
                sb.append("\n");
            }
            if (result.containsKey("supportResistance")) {
                sb.append("=== SUPPORT & RESISTANCE ===\n");
                Map<String, Object> supportResistance = (Map<String, Object>) result.get("supportResistance");
                List<Double> resistance = (List<Double>) supportResistance.get("resistance");
                if (resistance != null && !resistance.isEmpty()) {
                    sb.append("Resistance Levels:\n");
                    resistance.forEach(level -> sb.append(String.format("- $%.2f\n", level)));
                }
                List<Double> support = (List<Double>) supportResistance.get("support");
                if (support != null && !support.isEmpty()) {
                    sb.append("Support Levels:\n");
                    support.forEach(level -> sb.append(String.format("- $%.2f\n", level)));
                }
                sb.append("\n");
            }
            if (result.containsKey("marketSentiment")) {
                sb.append("=== MARKET SENTIMENT ===\n");
                Map<String, Object> sentiment = (Map<String, Object>) result.get("marketSentiment");
                sb.append(String.format("Bullish Signals: %s\n", sentiment.get("bullishSignals")));
                sb.append(String.format("Bearish Signals: %s\n", sentiment.get("bearishSignals")));
                sb.append(String.format("Sentiment Score: %.2f\n", sentiment.get("sentimentScore")));
                sb.append(String.format("Overall Sentiment: %s\n", sentiment.get("sentiment")));
                sb.append("\n");
            }
            if (result.containsKey("riskMetrics")) {
                sb.append("=== RISK METRICS ===\n");
                Map<String, Object> riskMetrics = (Map<String, Object>) result.get("riskMetrics");
                sb.append(String.format("Volatility: %.4f\n", riskMetrics.get("volatility")));
                sb.append(String.format("Risk Level: %s\n", riskMetrics.get("riskLevel")));
                sb.append(String.format("Max Drawdown: %.2f%%\n", riskMetrics.get("maxDrawdown")));
            }
            if (result.containsKey("error")) {
                sb.append(String.format("Error: %s\n", result.get("error")));
            }
        } catch (Exception e) {
            sb.append(String.format("Error formatting advanced analysis result: %s\n", e.getMessage()));
        }
        return sb.toString();
    }
} 