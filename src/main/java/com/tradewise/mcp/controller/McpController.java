package com.tradewise.mcp.controller;

import com.tradewise.mcp.config.McpConfig;
import com.tradewise.mcp.model.McpRequest;
import com.tradewise.mcp.model.McpResponse;
import com.tradewise.mcp.service.McpAnalysisService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/mcp")
@Slf4j
public class McpController {
    
    private final McpConfig mcpConfig;
    private final McpAnalysisService mcpAnalysisService;
    
    public McpController(McpConfig mcpConfig, McpAnalysisService mcpAnalysisService) {
        this.mcpConfig = mcpConfig;
        this.mcpAnalysisService = mcpAnalysisService;
    }
    
    /**
     * MCP initialization endpoint
     */
    @PostMapping("/initialize")
    public Mono<ResponseEntity<McpResponse>> initialize(@RequestBody McpRequest request) {
        log.info("MCP initialization request received");
        
        if (!mcpConfig.isEnabled()) {
            return Mono.just(ResponseEntity.ok(createErrorResponse(request.getId(), 
                    -1, "MCP server is disabled")));
        }
        
        List<McpResponse.Tool> tools = List.of(
                McpResponse.Tool.builder()
                        .name("technical_analysis")
                        .description("Performs comprehensive technical analysis on a stock symbol")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "symbol", Map.of(
                                                "type", "string",
                                                "description", "Stock symbol to analyze"
                                        )
                                ),
                                "required", List.of("symbol")
                        ))
                        .build(),
                
                McpResponse.Tool.builder()
                        .name("historical_analysis")
                        .description("Retrieves historical stock data with technical indicators")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "symbol", Map.of(
                                                "type", "string",
                                                "description", "Stock symbol to analyze"
                                        ),
                                        "days", Map.of(
                                                "type", "integer",
                                                "description", "Number of days of historical data to retrieve",
                                                "default", mcpConfig.getTools().getDefaultAnalysisDays()
                                        )
                                ),
                                "required", List.of("symbol")
                        ))
                        .build(),
                
                McpResponse.Tool.builder()
                        .name("advanced_analysis")
                        .description("Performs advanced pattern recognition and market sentiment analysis")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "symbol", Map.of(
                                                "type", "string",
                                                "description", "Stock symbol to analyze"
                                        ),
                                        "days", Map.of(
                                                "type", "integer",
                                                "description", "Number of days for analysis",
                                                "default", mcpConfig.getTools().getDefaultAnalysisDays()
                                        )
                                ),
                                "required", List.of("symbol")
                        ))
                        .build(),
                
                McpResponse.Tool.builder()
                        .name("current_price")
                        .description("Gets current stock price and basic information")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "symbol", Map.of(
                                                "type", "string",
                                                "description", "Stock symbol to get current price for"
                                        )
                                ),
                                "required", List.of("symbol")
                        ))
                        .build(),
                
                McpResponse.Tool.builder()
                        .name("search_stocks")
                        .description("Searches for stock symbols based on query")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of(
                                                "type", "string",
                                                "description", "Search query for stock symbols"
                                        )
                                ),
                                "required", List.of("query")
                        ))
                        .build()
        );
        
        McpResponse.ServerInfo serverInfo = McpResponse.ServerInfo.builder()
                .name(mcpConfig.getServerName())
                .version(mcpConfig.getServerVersion())
                .description(mcpConfig.getServerDescription())
                .tools(tools)
                .build();
        
        McpResponse response = McpResponse.builder()
                .id(request.getId())
                .result(serverInfo)
                .build();
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    /**
     * MCP tool execution endpoint
     */
    @PostMapping("/tools/call")
    public Mono<ResponseEntity<McpResponse>> callTool(@RequestBody McpRequest request) {
        log.info("MCP tool call request received: {}", request.getMethod());
        
        if (!mcpConfig.isEnabled()) {
            return Mono.just(ResponseEntity.ok(createErrorResponse(request.getId(), 
                    -1, "MCP server is disabled")));
        }
        
        if (request.getParams() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.getParams();
            String toolName = (String) params.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
            
            return executeToolCall(request.getId(), toolName, arguments);
        }
        
        return Mono.just(ResponseEntity.ok(createErrorResponse(request.getId(), 
                -32602, "Invalid params")));
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", mcpConfig.isEnabled() ? "UP" : "DOWN");
        health.put("serverName", mcpConfig.getServerName());
        health.put("serverVersion", mcpConfig.getServerVersion());
        health.put("toolsEnabled", Map.of(
                "technicalAnalysis", mcpConfig.getTools().isTechnicalAnalysis(),
                "historicalAnalysis", mcpConfig.getTools().isHistoricalAnalysis(),
                "advancedAnalysis", true, // Always enabled as it's a composite of other analyses
                "currentPrice", mcpConfig.getTools().isCurrentPrice(),
                "stockSearch", mcpConfig.getTools().isStockSearch()
        ));
        
        return Mono.just(ResponseEntity.ok(health));
    }
    
    private Mono<ResponseEntity<McpResponse>> executeToolCall(String requestId, String toolName, 
                                                            Map<String, Object> arguments) {
        switch (toolName) {
            case "technical_analysis":
                return executeTechnicalAnalysis(requestId, arguments);
            case "historical_analysis":
                return executeHistoricalAnalysis(requestId, arguments);
            case "advanced_analysis":
                return executeAdvancedAnalysis(requestId, arguments);
            case "current_price":
                return executeCurrentPrice(requestId, arguments);
            case "search_stocks":
                return executeSearchStocks(requestId, arguments);
            default:
                return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                        -32601, "Unknown tool: " + toolName)));
        }
    }
    
    private Mono<ResponseEntity<McpResponse>> executeTechnicalAnalysis(String requestId, 
                                                                      Map<String, Object> arguments) {
        String symbol = (String) arguments.get("symbol");
        if (symbol == null || symbol.trim().isEmpty()) {
            return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                    -32602, "Symbol is required")));
        }
        
        return mcpAnalysisService.performTechnicalAnalysis(symbol)
                .map(result -> {
                    McpResponse.ToolResult toolResult = McpResponse.ToolResult.builder()
                            .content(formatTechnicalAnalysisResult(result))
                            .isError(false)
                            .meta(Map.of("symbol", symbol, "type", "technical_analysis"))
                            .build();
                    
                    return ResponseEntity.ok(McpResponse.builder()
                            .id(requestId)
                            .result(toolResult)
                            .build());
                })
                .onErrorResume(e -> {
                    log.error("Error executing technical analysis for symbol: {}", symbol, e);
                    return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                            -32603, "Technical analysis failed: " + e.getMessage())));
                });
    }
    
    private Mono<ResponseEntity<McpResponse>> executeHistoricalAnalysis(String requestId, 
                                                                       Map<String, Object> arguments) {
        String symbol = (String) arguments.get("symbol");
        Integer days = (Integer) arguments.getOrDefault("days", mcpConfig.getTools().getDefaultAnalysisDays());
        
        if (symbol == null || symbol.trim().isEmpty()) {
            return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                    -32602, "Symbol is required")));
        }
        
        return mcpAnalysisService.getHistoricalAnalysis(symbol, days)
                .map(result -> {
                    McpResponse.ToolResult toolResult = McpResponse.ToolResult.builder()
                            .content(formatHistoricalAnalysisResult(result))
                            .isError(false)
                            .meta(Map.of("symbol", symbol, "days", days, "type", "historical_analysis"))
                            .build();
                    
                    return ResponseEntity.ok(McpResponse.builder()
                            .id(requestId)
                            .result(toolResult)
                            .build());
                })
                .onErrorResume(e -> {
                    log.error("Error executing historical analysis for symbol: {}", symbol, e);
                    return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                            -32603, "Historical analysis failed: " + e.getMessage())));
                });
    }
    
    private Mono<ResponseEntity<McpResponse>> executeAdvancedAnalysis(String requestId, 
                                                                     Map<String, Object> arguments) {
        String symbol = (String) arguments.get("symbol");
        Integer days = (Integer) arguments.getOrDefault("days", mcpConfig.getTools().getDefaultAnalysisDays());
        
        if (symbol == null || symbol.trim().isEmpty()) {
            return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                    -32602, "Symbol is required")));
        }
        
        return mcpAnalysisService.performAdvancedAnalysis(symbol, days)
                .map(result -> {
                    McpResponse.ToolResult toolResult = McpResponse.ToolResult.builder()
                            .content(formatAdvancedAnalysisResult(result))
                            .isError(false)
                            .meta(Map.of("symbol", symbol, "days", days, "type", "advanced_analysis"))
                            .build();
                    
                    return ResponseEntity.ok(McpResponse.builder()
                            .id(requestId)
                            .result(toolResult)
                            .build());
                })
                .onErrorResume(e -> {
                    log.error("Error executing advanced analysis for symbol: {}", symbol, e);
                    return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                            -32603, "Advanced analysis failed: " + e.getMessage())));
                });
    }
    
    private Mono<ResponseEntity<McpResponse>> executeCurrentPrice(String requestId, 
                                                                 Map<String, Object> arguments) {
        String symbol = (String) arguments.get("symbol");
        if (symbol == null || symbol.trim().isEmpty()) {
            return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                    -32602, "Symbol is required")));
        }
        
        return mcpAnalysisService.performTechnicalAnalysis(symbol)
                .map(result -> {
                    String content = String.format("Current price for %s: $%.2f", 
                            symbol, result.getCurrentPrice());
                    
                    McpResponse.ToolResult toolResult = McpResponse.ToolResult.builder()
                            .content(content)
                            .isError(false)
                            .meta(Map.of("symbol", symbol, "price", result.getCurrentPrice(), 
                                    "type", "current_price"))
                            .build();
                    
                    return ResponseEntity.ok(McpResponse.builder()
                            .id(requestId)
                            .result(toolResult)
                            .build());
                })
                .onErrorResume(e -> {
                    log.error("Error getting current price for symbol: {}", symbol, e);
                    return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                            -32603, "Current price lookup failed: " + e.getMessage())));
                });
    }
    
    private Mono<ResponseEntity<McpResponse>> executeSearchStocks(String requestId, 
                                                                 Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        if (query == null || query.trim().isEmpty()) {
            return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                    -32602, "Query is required")));
        }
        
        return mcpAnalysisService.searchStocks(query)
                .map(results -> {
                    String content = "Search results for '" + query + "':\n";
                    for (Map<String, Object> result : results) {
                        content += String.format("- %s (%s): %s [%s]\n", 
                                result.get("symbol"), result.get("market"), 
                                result.get("name"), result.get("status"));
                    }
                    
                    McpResponse.ToolResult toolResult = McpResponse.ToolResult.builder()
                            .content(content)
                            .isError(false)
                            .meta(Map.of("query", query, "results", results.size(), 
                                    "type", "search_stocks"))
                            .build();
                    
                    return ResponseEntity.ok(McpResponse.builder()
                            .id(requestId)
                            .result(toolResult)
                            .build());
                })
                .onErrorResume(e -> {
                    log.error("Error searching stocks with query: {}", query, e);
                    return Mono.just(ResponseEntity.ok(createErrorResponse(requestId, 
                            -32603, "Stock search failed: " + e.getMessage())));
                });
    }
    
    private McpResponse createErrorResponse(String requestId, int code, String message) {
        return McpResponse.builder()
                .id(requestId)
                .error(McpResponse.McpError.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }
    
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
            
            // Analysis metadata
            sb.append(String.format("Analysis Period: %s\n", result.get("period")));
            sb.append(String.format("Data Points: %s\n", result.get("dataPoints")));
            sb.append(String.format("Timestamp: %s\n\n", result.get("analysisTimestamp")));
            
            // Price patterns
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
            
            // Volume patterns
            if (result.containsKey("volumePatterns")) {
                sb.append("=== VOLUME PATTERNS ===\n");
                Map<String, Object> volumePatterns = (Map<String, Object>) result.get("volumePatterns");
                sb.append(String.format("Volume Trend: %s\n", volumePatterns.get("volumeTrend")));
                sb.append(String.format("Volume-Price Relation: %s\n", volumePatterns.get("volumePriceRelation")));
                sb.append("\n");
            }
            
            // Candlestick patterns
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
            
            // Trend analysis
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
            
            // Support and resistance
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
            
            // Market sentiment
            if (result.containsKey("marketSentiment")) {
                sb.append("=== MARKET SENTIMENT ===\n");
                Map<String, Object> sentiment = (Map<String, Object>) result.get("marketSentiment");
                sb.append(String.format("Bullish Signals: %s\n", sentiment.get("bullishSignals")));
                sb.append(String.format("Bearish Signals: %s\n", sentiment.get("bearishSignals")));
                sb.append(String.format("Sentiment Score: %.2f\n", sentiment.get("sentimentScore")));
                sb.append(String.format("Overall Sentiment: %s\n", sentiment.get("sentiment")));
                sb.append("\n");
            }
            
            // Risk metrics
            if (result.containsKey("riskMetrics")) {
                sb.append("=== RISK METRICS ===\n");
                Map<String, Object> riskMetrics = (Map<String, Object>) result.get("riskMetrics");
                sb.append(String.format("Volatility: %.4f\n", riskMetrics.get("volatility")));
                sb.append(String.format("Risk Level: %s\n", riskMetrics.get("riskLevel")));
                sb.append(String.format("Max Drawdown: %.2f%%\n", riskMetrics.get("maxDrawdown")));
            }
            
            // Error handling
            if (result.containsKey("error")) {
                sb.append(String.format("Error: %s\n", result.get("error")));
            }
            
        } catch (Exception e) {
            sb.append(String.format("Error formatting advanced analysis result: %s\n", e.getMessage()));
        }
        
        return sb.toString();
    }
}
