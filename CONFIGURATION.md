# Configuration Guide

This document explains how to configure the TradeWise AI application using the `TradeWiseProps` class.

## Configuration Properties

The application uses the `TradeWiseProps` class to read configuration from `application.properties` or `application.yml` files.

### AlphaVantage API Configuration

```properties
# AlphaVantage API Configuration
tradewise.alpha-vantage.api-key=YOUR_API_KEY_HERE
tradewise.alpha-vantage.base-url=https://www.alphavantage.co/query
tradewise.alpha-vantage.symbol-suffix=.BSE
tradewise.alpha-vantage.timeout-seconds=30
```

### Cache Configuration

```properties
# Cache Configuration
tradewise.cache.ttl-minutes=60
tradewise.cache.enabled=true
```

### Cosmos DB Configuration

```properties
# Cosmos DB Configuration (disabled by default)
tradewise.cosmos-db.enabled=false
tradewise.cosmos-db.endpoint=https://your-cosmos-account.documents.azure.com:443/
tradewise.cosmos-db.key=YOUR_COSMOS_DB_KEY_HERE
tradewise.cosmos-db.database-name=tradewise-db
tradewise.cosmos-db.container-name=stock-data
```

## Environment Variables

You can also use environment variables or the `application.yml.template` file:

```yaml
tradewise:
  alpha-vantage:
    api-key: ${ALPHA_VANTAGE_API_KEY:YOUR_API_KEY_HERE}
    base-url: ${ALPHA_VANTAGE_BASE_URL:https://www.alphavantage.co/query}
    symbol-suffix: ${ALPHA_VANTAGE_SYMBOL_SUFFIX:.BSE}
    timeout-seconds: ${ALPHA_VANTAGE_TIMEOUT:30}
  
  cache:
    ttl-minutes: ${CACHE_TTL_MINUTES:60}
    enabled: ${CACHE_ENABLED:true}
  
  cosmos-db:
    enabled: ${COSMOS_DB_ENABLED:false}
    endpoint: ${COSMOS_DB_ENDPOINT:https://your-cosmos-account.documents.azure.com:443/}
    key: ${COSMOS_DB_KEY:YOUR_COSMOS_DB_KEY_HERE}
    database-name: ${COSMOS_DB_DATABASE:tradewise-db}
    container-name: ${COSMOS_DB_CONTAINER:stock-data}
```

## Usage in Code

The `TradeWiseProps` class is automatically injected into other components:

```java
@Service
public class SomeService {
    
    private final TradeWiseProps tradeWiseProps;
    
    public SomeService(TradeWiseProps tradeWiseProps) {
        this.tradeWiseProps = tradeWiseProps;
    }
    
    public void someMethod() {
        String apiKey = tradeWiseProps.getAlphaVantage().getApiKey();
        String baseUrl = tradeWiseProps.getAlphaVantage().getBaseUrl();
        boolean cacheEnabled = tradeWiseProps.getCache().isEnabled();
        // ... use configuration
    }
}
```

## Configuration Classes

- `TradeWiseProps`: Main configuration class
- `TradeWiseProps.AlphaVantage`: AlphaVantage API settings
- `TradeWiseProps.CosmosDb`: Cosmos DB settings
- `TradeWiseProps.Cache`: Cache settings

## Default Values

The application provides sensible defaults for most configuration properties:

- **AlphaVantage Base URL**: `https://www.alphavantage.co/query`
- **Symbol Suffix**: `.BSE`
- **Timeout**: `30 seconds`
- **Cache TTL**: `60 minutes`
- **Cache Enabled**: `true`
- **Cosmos DB Enabled**: `false`
- **Database Name**: `tradewise-db`
- **Container Name**: `stock-data`

## Security Notes

- Never commit API keys to version control
- Use environment variables for sensitive configuration
- Consider using Spring Boot's configuration encryption for production
