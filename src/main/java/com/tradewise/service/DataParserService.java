package com.tradewise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradewise.model.DailyData;
import com.tradewise.model.StockData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@Service
@Slf4j
public class DataParserService {

    private final ObjectMapper objectMapper;
    
    public DataParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Parse AlphaVantage historical JSON data into StockData model
     */
    public StockData parseHistoricalData(String jsonData, String symbol) {
        
        try {
            log.info("Parsing historical data for symbol: {}", symbol);
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode timeSeries = rootNode.path("Time Series (Daily)");
            
            if (timeSeries.isMissingNode()) {
                log.warn("No time series data found for symbol: {}", symbol);
                return StockData.builder()
                        .stockSymbol(symbol)
                        .dataType("HISTORICAL")
                        .dailyData(new ArrayList<>())
                        .lastUpdated(java.time.LocalDateTime.now())
                        .build();
            }
            
            List<DailyData> dailyDataList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Parse each date entry
            timeSeries.fieldNames().forEachRemaining(dateStr -> {
                try {
                    JsonNode dayData = timeSeries.get(dateStr);
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    
                    // Parse string values to BigDecimal and Long with proper validation
                    DailyData dailyData = DailyData.builder()
                            .date(date)
                            .openPrice(parseStringToBigDecimal(dayData.get("1. open").asText()))
                            .highPrice(parseStringToBigDecimal(dayData.get("2. high").asText()))
                            .lowPrice(parseStringToBigDecimal(dayData.get("3. low").asText()))
                            .closePrice(parseStringToBigDecimal(dayData.get("4. close").asText()))
                            .volume(parseStringToLong(dayData.get("5. volume").asText()))
                            .build();
                    
                    dailyDataList.add(dailyData);
                    log.debug("Parsed daily data for {}: Open={}, High={}, Low={}, Close={}, Volume={}", 
                            dateStr, dailyData.getOpenPrice(), dailyData.getHighPrice(), 
                            dailyData.getLowPrice(), dailyData.getClosePrice(), dailyData.getVolume());
                } catch (Exception e) {
                    log.error("Error parsing daily data for date: {}", dateStr, e);
                }
            });
            
            // Sort by date in ascending order
            dailyDataList.sort(Comparator.comparing(DailyData::getDate));
            
            log.info("Successfully parsed {} daily data entries for symbol: {}", dailyDataList.size(), symbol);
            
            return StockData.builder()
                    .stockSymbol(symbol)
                    .dailyData(dailyDataList)
                    .dataType("HISTORICAL")
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build();

                    
        } catch (Exception e) {
            log.error("Error parsing historical data for symbol: {}", symbol, e);
            throw new RuntimeException("Failed to parse historical data", e);
        }
    }
    
    /**
     * Parse AlphaVantage current price JSON data into StockData model
     */
    public StockData parseCurrentPriceData(String jsonData, String symbol) {
        try {
            log.info("Parsing current price data for symbol: {}", symbol);
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode globalQuote = rootNode.path("Global Quote");
            
            if (globalQuote.isMissingNode()) {
                log.warn("No global quote data found for symbol: {}", symbol);
                return StockData.builder()
                        .stockSymbol(symbol)
                        .dataType("CURRENT")
                        .dailyData(new ArrayList<>())
                        .lastUpdated(java.time.LocalDateTime.now())
                        .build();
            }
            
            // Create a single day data entry for current price
            DailyData currentData = DailyData.builder()
                    .date(LocalDate.now())
                    .openPrice(parseStringToBigDecimal(globalQuote.get("02. open").asText()))
                    .highPrice(parseStringToBigDecimal(globalQuote.get("03. high").asText()))
                    .lowPrice(parseStringToBigDecimal(globalQuote.get("04. low").asText()))
                    .closePrice(parseStringToBigDecimal(globalQuote.get("05. price").asText()))
                    .volume(parseStringToLong(globalQuote.get("06. volume").asText()))
                    .build();
            
            List<DailyData> dailyDataList = new ArrayList<>();
            dailyDataList.add(currentData);
            
            log.info("Successfully parsed current price data for symbol: {} - Close: {}", 
                    symbol, currentData.getClosePrice());
            
            return StockData.builder()
                    .stockSymbol(symbol)
                    .dailyData(dailyDataList)
                    .dataType("CURRENT")
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error parsing current price data for symbol: {}", symbol, e);
            throw new RuntimeException("Failed to parse current price data", e);
        }
    }
    
    /**
     * Safely parse string to BigDecimal
     */
    private java.math.BigDecimal parseStringToBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal from value: {}, defaulting to 0", value);
            return java.math.BigDecimal.ZERO;
        }
    }
    
    /**
     * Safely parse string to Long
     */
    private Long parseStringToLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long from value: {}, defaulting to 0", value);
            return 0L;
        }
    }
}
