package com.tradewise.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyData {
    
    private LocalDate date;
    
    private BigDecimal openPrice;
    
    private BigDecimal highPrice;
    
    private BigDecimal lowPrice;
    
    private BigDecimal closePrice;
    
    private Long volume;
    
    // Constructor for easy creation from AlphaVantage data
    public DailyData(LocalDate date, String open, String high, String low, String close, String volume) {
        this.date = date;
        this.openPrice = parseStringToBigDecimal(open);
        this.highPrice = parseStringToBigDecimal(high);
        this.lowPrice = parseStringToBigDecimal(low);
        this.closePrice = parseStringToBigDecimal(close);
        this.volume = parseStringToLong(volume);
    }
    
    private BigDecimal parseStringToBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    private Long parseStringToLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
