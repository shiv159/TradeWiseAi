package com.tradewise.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicalAnalysisResult {
    private String symbol;
    private Double currentPrice;
    private Double rsi;
    private Double sma14;
    private String trend;
    private String signal;
    private Integer dataPoints;
    private String lastUpdated;
    private Double open;
    private Double high;
    private Double low;
    private Long volume;
}
