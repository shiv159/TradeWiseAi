package com.tradewise.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyData {
    
    private LocalDate date;
    
    @JsonProperty("1. open")
    private BigDecimal openPrice;
    
    @JsonProperty("2. high")
    private BigDecimal highPrice;
    
    @JsonProperty("3. low")
    private BigDecimal lowPrice;
    
    @JsonProperty("4. close")
    private BigDecimal closePrice;
    
    @JsonProperty("5. volume")
    private Long volume;
    
    // Constructor for easy creation from AlphaVantage data
    public DailyData(LocalDate date, String open, String high, String low, String close, String volume) {
        this.date = date;
        this.openPrice = new BigDecimal(open);
        this.highPrice = new BigDecimal(high);
        this.lowPrice = new BigDecimal(low);
        this.closePrice = new BigDecimal(close);
        this.volume = Long.parseLong(volume);
    }
}
