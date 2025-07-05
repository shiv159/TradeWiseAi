package com.tradewise.service;

import com.tradewise.configs.TradeWiseProps;
import com.tradewise.model.DailyData;
import com.tradewise.model.StockData;
import com.tradewise.repository.StockDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class MongoDBIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "tradewise-test");
    }

    @Autowired
    private StockDataRepository stockDataRepository;

    @Autowired
    private TradeWiseProps tradeWiseProps;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        stockDataRepository.deleteAll().block();
    }

    @Test
    void testSaveAndFindStockData() {
        // Given
        String symbol = "AAPL";
        StockData stockData = createSampleStockData(symbol, "HISTORICAL");

        // When
        Mono<StockData> savedData = stockDataRepository.save(stockData);

        // Then
        StepVerifier.create(savedData)
                .assertNext(saved -> {
                    assertNotNull(saved.getId());
                    assertEquals(symbol, saved.getStockSymbol());
                    assertEquals("HISTORICAL", saved.getDataType());
                    assertNotNull(saved.getLastUpdated());
                    assertEquals(1, saved.getDailyData().size());
                })
                .verifyComplete();
    }

    @Test
    void testFindByStockSymbolAndDataType() {
        // Given
        String symbol = "AAPL";
        StockData historicalData = createSampleStockData(symbol, "HISTORICAL");
        StockData currentData = createSampleStockData(symbol, "CURRENT");

        // When
        Mono<StockData> result = stockDataRepository.save(historicalData)
                .then(stockDataRepository.save(currentData))
                .then(stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL"));

        // Then
        StepVerifier.create(result)
                .assertNext(found -> {
                    assertEquals(symbol, found.getStockSymbol());
                    assertEquals("HISTORICAL", found.getDataType());
                })
                .verifyComplete();
    }

    @Test
    void testSaveOrUpdateMethod() {
        // Given
        String symbol = "AAPL";
        StockData originalData = createSampleStockData(symbol, "HISTORICAL");
        originalData.setLastUpdated(LocalDateTime.now().minusHours(2));

        // Save original data
        stockDataRepository.save(originalData).block();

        // Create updated data
        StockData updatedData = createSampleStockData(symbol, "HISTORICAL");
        updatedData.getDailyData().add(DailyData.builder()
                .date(LocalDate.now().minusDays(1))
                .openPrice(new BigDecimal("151.00"))
                .highPrice(new BigDecimal("155.00"))
                .lowPrice(new BigDecimal("150.00"))
                .closePrice(new BigDecimal("154.00"))
                .volume(2000000L)
                .build());

        // When
        Mono<StockData> result = stockDataRepository.saveOrUpdate(updatedData);

        // Then
        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertEquals(symbol, saved.getStockSymbol());
                    assertEquals("HISTORICAL", saved.getDataType());
                    assertEquals(2, saved.getDailyData().size());
                    assertNotNull(saved.getLastUpdated());
                })
                .verifyComplete();

        // Verify only one document exists for this symbol and data type
        StepVerifier.create(stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL"))
                .assertNext(found -> {
                    assertEquals(2, found.getDailyData().size());
                })
                .verifyComplete();
    }

    @Test
    void testDeleteByStockSymbolAndDataType() {
        // Given
        String symbol = "AAPL";
        StockData historicalData = createSampleStockData(symbol, "HISTORICAL");
        StockData currentData = createSampleStockData(symbol, "CURRENT");

        // Save both data types
        stockDataRepository.save(historicalData).block();
        stockDataRepository.save(currentData).block();

        // When
        Mono<Void> deleteResult = stockDataRepository.deleteByStockSymbolAndDataType(symbol, "HISTORICAL");

        // Then
        StepVerifier.create(deleteResult)
                .verifyComplete();

        // Verify historical data is deleted but current data remains
        StepVerifier.create(stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL"))
                .verifyComplete();

        StepVerifier.create(stockDataRepository.findByStockSymbolAndDataType(symbol, "CURRENT"))
                .assertNext(found -> {
                    assertEquals(symbol, found.getStockSymbol());
                    assertEquals("CURRENT", found.getDataType());
                })
                .verifyComplete();
    }

    @Test
    void testExistsByStockSymbolAndDataType() {
        // Given
        String symbol = "AAPL";
        StockData stockData = createSampleStockData(symbol, "HISTORICAL");

        // When
        Mono<Boolean> existsBeforeSave = stockDataRepository.existsByStockSymbolAndDataType(symbol, "HISTORICAL");
        Mono<Boolean> existsAfterSave = stockDataRepository.save(stockData)
                .then(stockDataRepository.existsByStockSymbolAndDataType(symbol, "HISTORICAL"));

        // Then
        StepVerifier.create(existsBeforeSave)
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(existsAfterSave)
                .expectNext(true)
                .verifyComplete();
    }

    private StockData createSampleStockData(String symbol, String dataType) {
        DailyData dailyData = DailyData.builder()
                .date(LocalDate.now())
                .openPrice(new BigDecimal("150.00"))
                .highPrice(new BigDecimal("155.00"))
                .lowPrice(new BigDecimal("148.00"))
                .closePrice(new BigDecimal("152.00"))
                .volume(1000000L)
                .build();

        return StockData.builder()
                .stockSymbol(symbol)
                .dataType(dataType)
                .dailyData(List.of(dailyData))
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
