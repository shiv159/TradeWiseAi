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
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode timeSeries = rootNode.path("Time Series (Daily)");
            
            if (timeSeries.isMissingNode()) {
                log.warn("No time series data found for symbol: {}", symbol);
                return StockData.builder()
                        .stockSymbol(symbol)
                        .dataType("HISTORICAL")
                        .build();
            }
            
            List<DailyData> dailyDataList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Parse each date entry
            timeSeries.fieldNames().forEachRemaining(dateStr -> {
                try {
                    JsonNode dayData = timeSeries.get(dateStr);
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    
                    DailyData dailyData = DailyData.builder()
                            .date(date)
                            .openPrice(dayData.get("1. open").decimalValue())
                            .highPrice(dayData.get("2. high").decimalValue())
                            .lowPrice(dayData.get("3. low").decimalValue())
                            .closePrice(dayData.get("4. close").decimalValue())
                            .volume(dayData.get("5. volume").longValue())
                            .build();
                    
                    dailyDataList.add(dailyData);
                } catch (Exception e) {
                    log.error("Error parsing daily data for date: {}", dateStr, e);
                }
            });
            
            // Sort by date in ascending order
            dailyDataList.sort(Comparator.comparing(DailyData::getDate));
            
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
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode globalQuote = rootNode.path("Global Quote");
            
            if (globalQuote.isMissingNode()) {
                log.warn("No global quote data found for symbol: {}", symbol);
                return StockData.builder()
                        .stockSymbol(symbol)
                        .dataType("CURRENT")
                        .build();
            }
            
            // Create a single day data entry for current price
            DailyData currentData = DailyData.builder()
                    .date(LocalDate.now())
                    .openPrice(globalQuote.get("02. open").decimalValue())
                    .highPrice(globalQuote.get("03. high").decimalValue())
                    .lowPrice(globalQuote.get("04. low").decimalValue())
                    .closePrice(globalQuote.get("05. price").decimalValue())
                    .volume(globalQuote.get("06. volume").longValue())
                    .build();
            
            List<DailyData> dailyDataList = new ArrayList<>();
            dailyDataList.add(currentData);
            
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
}
