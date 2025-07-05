package com.tradewise.repository;

import com.tradewise.model.StockData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface StockDataRepository extends ReactiveMongoRepository<StockData, String> {
    
    Mono<StockData> findByStockSymbolAndDataType(String stockSymbol, String dataType);
    
    Mono<StockData> findByStockSymbol(String stockSymbol);
    
    Mono<Void> deleteByStockSymbol(String stockSymbol);
    
    Mono<Void> deleteByStockSymbolAndDataType(String stockSymbol, String dataType);
    
    Mono<Boolean> existsByStockSymbolAndDataType(String stockSymbol, String dataType);
    
    // Custom method to save or update existing data
    default Mono<StockData> saveOrUpdate(StockData stockData) {
        return this.deleteByStockSymbolAndDataType(stockData.getStockSymbol(), stockData.getDataType())
                .then(this.save(stockData));
    }
}
