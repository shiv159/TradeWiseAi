package com.tradewise.mcp.controller;

import com.tradewise.mcp.service.McpAnalysisService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class McpTools {
    private final McpAnalysisService mcpAnalysisService;

    public McpTools(McpAnalysisService mcpAnalysisService) {
        this.mcpAnalysisService = mcpAnalysisService;
    }

    @Tool(name = "technical_analysis", description = "Performs comprehensive technical analysis on a stock symbol")
    public String technicalAnalysis(@ToolParam(description = "Stock symbol to analyze") String symbol) {
        return mcpAnalysisService.performTechnicalAnalysis(symbol).block();
    }

    @Tool(name = "historical_analysis", description = "Retrieves historical stock data with technical indicators")
    public String historicalAnalysis(@ToolParam(description = "Stock symbol to analyze") String symbol,
                                     @ToolParam(description = "Number of days of historical data to retrieve") Integer days) {
        return mcpAnalysisService.getHistoricalAnalysis(symbol, days).block();
    }

    @Tool(name = "advanced_analysis", description = "Performs advanced pattern recognition and market sentiment analysis")
    public String advancedAnalysis(@ToolParam(description = "Stock symbol to analyze") String symbol,
                                   @ToolParam(description = "Number of days for analysis") Integer days) {
        return mcpAnalysisService.performAdvancedAnalysis(symbol, days).block();
    }

    @Tool(name = "current_price", description = "Gets current stock price and basic information")
    public String currentPrice(@ToolParam(description = "Stock symbol to get current price for") String symbol) {
        return mcpAnalysisService.getCurrentPriceFormatted(symbol).block();
    }

    @Tool(name = "search_stocks", description = "Searches for stock symbols based on query")
    public String searchStocks(@ToolParam(description = "Search query for stock symbols") String query) {
        return mcpAnalysisService.searchStocksFormatted(query).block();
    }
} 