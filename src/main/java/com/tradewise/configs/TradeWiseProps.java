package com.tradewise.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tradewise")
public class TradeWiseProps {
    
    private AlphaVantage alphaVantage = new AlphaVantage();
    private CosmosDb cosmosDb = new CosmosDb();
    private Cache cache = new Cache();
    
    @Data
    public static class AlphaVantage {
        private String apiKey;
        private String baseUrl = "https://www.alphavantage.co/query";
        private String symbolSuffix = ".BSE";
        private int timeoutSeconds = 30;
    }
    
    @Data
    public static class CosmosDb {
        private String endpoint;
        private String key;
        private String databaseName;
        private String containerName;
        private boolean enabled = false;
    }
    
    @Data
    public static class Cache {
        private int ttlMinutes = 60;
        private boolean enabled = true;
    }
}
