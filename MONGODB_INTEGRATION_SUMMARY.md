# MongoDB Integration Summary

## Overview
The TradeWise AI application has been successfully integrated with MongoDB for optimized stock data retrieval. The implementation follows a cache-first approach where MongoDB is checked before making API calls to Alpha Vantage.

## Key Components

### 1. Data Models
- **StockData**: Main document structure with compound index on `stockSymbol` and `dataType`
- **DailyData**: Embedded document for daily trading data
- Both models use Lombok builders for efficient object creation

### 2. Repository Layer
- **StockDataRepository**: Reactive MongoDB repository with custom query methods
- Includes `saveOrUpdate()` method for efficient data updates
- Supports both current and historical data retrieval

### 3. Service Layer
- **TradeWiseService**: Implements cache-first strategy
  - Checks MongoDB before API calls
  - Automatically updates cache with new data
  - Handles both current and historical data requests
- **DataParserService**: Converts API responses to MongoDB documents with automatic timestamp setting

### 4. Document Structure
Each MongoDB document contains:
```json
{
  "stockSymbol": "AAPL",
  "dataType": "HISTORICAL", // or "CURRENT"
  "lastUpdated": "2025-07-06T12:00:00Z",
  "dailyData": [
    {
      "date": "2023-01-01",
      "openPrice": 150.0,
      "highPrice": 155.0,
      "lowPrice": 149.0,
      "closePrice": 154.0,
      "volume": 1000000
    }
  ]
}
```

## Flow Implementation

### Current Price Request
1. Check MongoDB for existing current data
2. If found and cache is valid, return cached data
3. If not found or cache expired, call Alpha Vantage API
4. Parse response and save to MongoDB
5. Return data to client

### Historical Data Request
1. Check MongoDB for existing historical data
2. If found and cache is valid, return cached data
3. If not found or cache expired, call Alpha Vantage API
4. Parse response and save to MongoDB
5. Return data to client

## Configuration
- Spring Data MongoDB Reactive is configured via `application.properties`
- Automatic indexing on `stockSymbol` and `dataType` fields
- Cache expiration logic built into service layer
- Reactive WebClient for non-blocking API calls

## Benefits
- **Performance**: Reduces API calls by caching frequently requested data
- **Cost Efficiency**: Minimizes Alpha Vantage API usage
- **Scalability**: Reactive MongoDB operations for high concurrency
- **Data Consistency**: Automatic timestamp tracking for cache invalidation
- **Flexibility**: Supports both current and historical data requests

## Dependencies
- spring-boot-starter-data-mongodb-reactive
- Spring WebFlux for reactive programming
- Jackson for JSON processing
- Lombok for reduced boilerplate code
