# MongoDB Integration for TradeWise AI

This document describes the MongoDB integration implemented in the TradeWise AI application for optimizing stock data retrieval.

## Overview

The application now uses MongoDB as a caching layer to reduce API calls to AlphaVantage. Before making external API calls, the system checks if the requested data is available in MongoDB. If found and fresh, it returns the cached data; otherwise, it fetches from the API and caches the result.

## Features

### 1. Intelligent Caching
- **Cache-first approach**: Checks MongoDB before making API calls
- **TTL-based freshness**: Configurable cache expiration (default: 60 minutes)
- **Automatic cache population**: Stores API responses in MongoDB for future use

### 2. Document Structure

#### StockData Document
```json
{
  "_id": "ObjectId",
  "stockSymbol": "AAPL",
  "dataType": "HISTORICAL", // or "CURRENT"
  "lastUpdated": "2023-12-01T10:30:00",
  "dailyData": [
    {
      "date": "2023-12-01",
      "openPrice": 150.0,
      "highPrice": 155.0,
      "lowPrice": 149.0,
      "closePrice": 154.0,
      "volume": 1000000
    }
  ]
}
```

### 3. Configuration

#### application.properties
```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/tradewise-db
spring.data.mongodb.database=tradewise-db

# Cache Configuration
tradewise.cache.ttl-minutes=60
tradewise.cache.enabled=true
```

## API Endpoints

### Get Current Price
```
GET /api/v1/getCurrentPrice?symbol=AAPL
```
- Checks MongoDB for fresh current price data
- If not found or stale, fetches from AlphaVantage API
- Returns formatted current price information

### Get Historical Price
```
GET /api/v1/historicalPrice?symbol=AAPL
```
- Checks MongoDB for fresh historical data
- If not found or stale, fetches from AlphaVantage API
- Performs technical analysis using TA4J library
- Returns RSI and SMA indicators

## Technical Implementation

### Key Components

1. **StockData Model**: Represents the document structure in MongoDB
2. **DailyData Model**: Represents individual trading day data
3. **StockDataRepository**: Reactive MongoDB repository interface
4. **DataParserService**: Converts AlphaVantage JSON to domain models
5. **TradeWiseService**: Main service with caching logic

### Caching Logic Flow

1. **Check Cache**: Look for data in MongoDB by symbol and data type
2. **Validate Freshness**: Check if cached data is within TTL
3. **Return Cached**: If fresh, return cached data
4. **Fetch from API**: If not cached or stale, call AlphaVantage API
5. **Parse and Store**: Convert API response to domain model and save to MongoDB
6. **Return Result**: Return processed data to client

## Dependencies Added

- `spring-boot-starter-data-mongodb-reactive`: Reactive MongoDB support
- `jackson-databind`: Enhanced JSON processing
- `jackson-datatype-jsr310`: Java 8 time support for JSON

## Database Setup

### Local MongoDB Setup
```bash
# Using Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest

# Using MongoDB Community Edition
# Follow MongoDB installation guide for your OS
```

### Production Setup
- Configure MongoDB connection string in `application.properties`
- Set up proper indexing for optimal query performance
- Consider MongoDB Atlas for cloud deployment

## Performance Benefits

1. **Reduced API Calls**: Significant reduction in external API requests
2. **Faster Response Times**: Cached data retrieval is much faster
3. **Cost Optimization**: Fewer API calls = lower AlphaVantage API costs
4. **Better User Experience**: Consistent response times

## Configuration Options

### Cache Settings
```properties
# Enable/disable caching
tradewise.cache.enabled=true

# Cache TTL in minutes
tradewise.cache.ttl-minutes=60
```

### MongoDB Settings
```properties
# MongoDB connection
spring.data.mongodb.uri=mongodb://localhost:27017/tradewise-db
spring.data.mongodb.database=tradewise-db
```

## Testing

Run the application tests to verify MongoDB integration:

```bash
./mvnw test
```

The test suite includes:
- Cache-enabled vs cache-disabled scenarios
- Data freshness validation
- API fallback behavior

## Monitoring

Monitor the application logs to track:
- Cache hit/miss ratios
- MongoDB operations
- API call frequency
- Data freshness checks

## Future Enhancements

1. **Index Optimization**: Add appropriate MongoDB indexes
2. **Cache Warming**: Pre-populate cache with popular stocks
3. **Real-time Updates**: WebSocket integration for live data
4. **Analytics Dashboard**: Monitor cache performance metrics
