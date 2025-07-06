package com.tradewise.mcp.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpResponse {
    
    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonRpc = "2.0";
    
    private String id;
    private Object result;
    private McpError error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpError {
        private int code;
        private String message;
        private Object data;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private String content;
        private boolean isError;
        private Map<String, Object> meta;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        private String name;
        private String version;
        private String description;
        private List<Tool> tools;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tool {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalAnalysisResult {
        private String symbol;
        private double rsi;
        private double sma;
        private double currentPrice;
        private String trend;
        private String signal;
        private int dataPoints;
        private String analysisDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalAnalysisResult {
        private String symbol;
        private List<DailyDataPoint> historicalData;
        private TechnicalIndicators indicators;
        private String period;
        private int totalDataPoints;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyDataPoint {
        private String date;
        private double open;
        private double high;
        private double low;
        private double close;
        private long volume;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalIndicators {
        private double rsi;
        private double sma14;
        private double sma50;
        private double ema12;
        private double ema26;
        private double macd;
        private double bollingerUpper;
        private double bollingerLower;
        private double stochasticK;
        private double stochasticD;
    }
}
