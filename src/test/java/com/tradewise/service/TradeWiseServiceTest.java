package com.tradewise.service;

import com.tradewise.configs.TradeWiseProps;
import com.tradewise.externalservicecaller.AlphaVantageServiceCaller;
import com.tradewise.model.DailyData;
import com.tradewise.model.StockData;
import com.tradewise.repository.StockDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeWiseServiceTest {

    @Mock
    private AlphaVantageServiceCaller alphaVantageServiceCaller;
    
    @Mock
    private StockDataRepository stockDataRepository;
    
    @Mock
    private DataParserService dataParserService;
    
    @Mock
    private TradeWiseProps tradeWiseProps;
    
    @Mock
    private TradeWiseProps.Cache cacheProps;
    
    private TradeWiseService tradeWiseService;
    
    @BeforeEach
    void setUp() {
        tradeWiseService = new TradeWiseService(
            alphaVantageServiceCaller,
            stockDataRepository,
            dataParserService,
            tradeWiseProps
        );
    }
    
    @Test
    void testGetCurrentPriceWithCacheDisabled() {
        // Given
        String symbol = "AAPL";
        String expectedResponse = "Current price: $150.00";
        
        when(tradeWiseProps.getCache()).thenReturn(cacheProps);
        when(cacheProps.isEnabled()).thenReturn(false);
        when(alphaVantageServiceCaller.getCurrentPrice(symbol))
            .thenReturn(Mono.just(expectedResponse));
        
        // When & Then
        StepVerifier.create(tradeWiseService.getCurrentPrice(symbol))
            .expectNext(expectedResponse)
            .verifyComplete();
    }
    
    @Test
    void testGetCurrentPriceWithCacheEnabled() {
        // Given
        String symbol = "AAPL";
        
        StockData cachedData = StockData.builder()
            .stockSymbol(symbol)
            .dataType("CURRENT")
            .lastUpdated(LocalDateTime.now().minusMinutes(30))
            .dailyData(List.of(
                DailyData.builder()
                    .date(LocalDate.now())
                    .closePrice(new BigDecimal("150.00"))
                    .openPrice(new BigDecimal("149.00"))
                    .highPrice(new BigDecimal("151.00"))
                    .lowPrice(new BigDecimal("148.00"))
                    .volume(1000000L)
                    .build()
            ))
            .build();
        
        when(tradeWiseProps.getCache()).thenReturn(cacheProps);
        when(cacheProps.isEnabled()).thenReturn(true);
        when(cacheProps.getTtlMinutes()).thenReturn(60);
        when(stockDataRepository.findByStockSymbolAndDataType(symbol, "CURRENT"))
            .thenReturn(Mono.just(cachedData));
        
        // When & Then
        StepVerifier.create(tradeWiseService.getCurrentPrice(symbol))
            .expectNextMatches(response -> response.contains("Current price for AAPL: $150.00"))
            .verifyComplete();
    }
    
    @Test
    void testGetHistoricalPriceWithCacheDisabled() {
        // Given
        String symbol = "AAPL";
        String historicalJson = "{\"Time Series (Daily)\": {}}";
        
        when(tradeWiseProps.getCache()).thenReturn(cacheProps);
        when(cacheProps.isEnabled()).thenReturn(false);
        when(alphaVantageServiceCaller.getHistoricalPrice(symbol))
            .thenReturn(Mono.just(historicalJson));
        
        // When & Then
        StepVerifier.create(tradeWiseService.getHistoricalPrice(symbol))
            .expectNextMatches(response -> response.startsWith("Error fetching historical price:"))
            .verifyComplete();
    }
}
