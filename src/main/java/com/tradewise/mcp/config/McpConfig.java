package com.tradewise.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "tradewise.mcp")
public class McpConfig {
    
    private String serverName = "TradeWise-MCP";
    private String serverVersion = "1.0.0";
    private String serverDescription = "MCP server for TradeWise AI stock analysis";
    private int port = 8082;
    private boolean enabled = true;
    
    // Tool configurations
    private Tools tools = new Tools();
    
    @Data
    public static class Tools {
        private boolean historicalAnalysis = true;
        private boolean technicalAnalysis = true;
        private boolean currentPrice = true;
        private boolean stockSearch = true;
        private int maxDataPoints = 1000;
        private int defaultAnalysisDays = 30;
    }
}
