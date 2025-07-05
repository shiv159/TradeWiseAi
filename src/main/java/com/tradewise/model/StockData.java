package com.tradewise.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "stock_data")
public class StockData {
    
    @Id
    private String id;
    
    @Indexed
    private String stockSymbol;
    
    @Builder.Default
    private List<DailyData> dailyData = new ArrayList<>();
    
    private LocalDateTime lastUpdated;
    
    @Indexed
    private String dataType; // "HISTORICAL" or "CURRENT"
    
    // Constructor for easy creation
    public StockData(String stockSymbol, List<DailyData> dailyData, String dataType) {
        this.stockSymbol = stockSymbol;
        this.dailyData = dailyData != null ? dailyData : new ArrayList<>();
        this.dataType = dataType;
        this.lastUpdated = LocalDateTime.now();
    }
}
