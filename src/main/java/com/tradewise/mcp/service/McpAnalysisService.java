package com.tradewise.mcp.service;

import com.tradewise.mcp.model.McpResponse;
import com.tradewise.service.TradeWiseService;
import com.tradewise.model.StockData;
import com.tradewise.model.DailyData;
import com.tradewise.repository.StockDataRepository;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@Slf4j
public class McpAnalysisService {
    
    private final TradeWiseService tradeWiseService;
    private final StockDataRepository stockDataRepository;
    
    public McpAnalysisService(TradeWiseService tradeWiseService, 
                             StockDataRepository stockDataRepository) {
        this.tradeWiseService = tradeWiseService;
        this.stockDataRepository = stockDataRepository;
    }
    
    /**
     * Performs comprehensive technical analysis for MCP clients
     */
    public Mono<McpResponse.TechnicalAnalysisResult> performTechnicalAnalysis(String symbol) {
        log.info("Performing technical analysis for symbol: {}", symbol);
        
        return tradeWiseService.getEnhancedTechnicalAnalysis(symbol)
                .map(indicators -> {
                    if (indicators.containsKey("error")) {
                        return McpResponse.TechnicalAnalysisResult.builder()
                                .symbol(symbol)
                                .rsi(0.0)
                                .sma(0.0)
                                .currentPrice(0.0)
                                .trend("ERROR")
                                .signal("ERROR: " + indicators.get("error"))
                                .dataPoints(0)
                                .analysisDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                                .build();
                    }
                    
                    return McpResponse.TechnicalAnalysisResult.builder()
                            .symbol(symbol)
                            .rsi(indicators.get("rsi") != null ? (Double) indicators.get("rsi") : 0.0)
                            .sma(indicators.get("sma14") != null ? (Double) indicators.get("sma14") : 0.0)
                            .currentPrice(indicators.get("currentPrice") != null ? (Double) indicators.get("currentPrice") : 0.0)
                            .trend(indicators.get("trend") != null ? (String) indicators.get("trend") : "UNKNOWN")
                            .signal(indicators.get("signal") != null ? (String) indicators.get("signal") : "No signal")
                            .dataPoints(indicators.get("dataPoints") != null ? (Integer) indicators.get("dataPoints") : 0)
                            .analysisDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error performing technical analysis for symbol: {}", symbol, e);
                    return Mono.just(McpResponse.TechnicalAnalysisResult.builder()
                            .symbol(symbol)
                            .rsi(0.0)
                            .sma(0.0)
                            .currentPrice(0.0)
                            .trend("ERROR")
                            .signal("ERROR: " + e.getMessage())
                            .dataPoints(0)
                            .analysisDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            .build());
                });
    }
    
    /**
     * Retrieves historical data with technical indicators for MCP clients
     */
    public Mono<McpResponse.HistoricalAnalysisResult> getHistoricalAnalysis(String symbol, int days) {
        log.info("Getting historical analysis for symbol: {} for {} days", symbol, days);
        
        return stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL")
                .switchIfEmpty(fetchHistoricalData(symbol))
                .map(stockData -> {
                    List<DailyData> limitedData = stockData.getDailyData().stream()
                            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                            .limit(days)
                            .collect(Collectors.toList());
                    
                    List<McpResponse.DailyDataPoint> dataPoints = limitedData.stream()
                            .map(this::convertToDataPoint)
                            .collect(Collectors.toList());
                    
                    McpResponse.TechnicalIndicators indicators = calculateAllIndicators(limitedData);
                    
                    return McpResponse.HistoricalAnalysisResult.builder()
                            .symbol(symbol)
                            .historicalData(dataPoints)
                            .indicators(indicators)
                            .period(days + " days")
                            .totalDataPoints(dataPoints.size())
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error getting historical analysis for symbol: {}", symbol, e);
                    return Mono.just(McpResponse.HistoricalAnalysisResult.builder()
                            .symbol(symbol)
                            .historicalData(List.of())
                            .indicators(McpResponse.TechnicalIndicators.builder().build())
                            .period("ERROR")
                            .totalDataPoints(0)
                            .build());
                });
    }
    
    /**
     * Searches for stock symbols and provides basic information
     */
    public Mono<List<Map<String, Object>>> searchStocks(String query) {
        log.info("Searching for stocks with query: {}", query);
        
        // For now, return a simple response. In a real implementation, 
        // you might integrate with a stock search API
        return Mono.just(List.of(
                Map.of(
                        "symbol", query.toUpperCase(),
                        "name", "Stock: " + query.toUpperCase(),
                        "market", "BSE",
                        "status", "active"
                )
        ));
    }
    
    private Mono<StockData> fetchHistoricalData(String symbol) {
        return tradeWiseService.getHistoricalPrice(symbol)
                .then(stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL"));
    }
    
    private McpResponse.DailyDataPoint convertToDataPoint(DailyData dailyData) {
        return McpResponse.DailyDataPoint.builder()
                .date(dailyData.getDate().toString())
                .open(dailyData.getOpenPrice().doubleValue())
                .high(dailyData.getHighPrice().doubleValue())
                .low(dailyData.getLowPrice().doubleValue())
                .close(dailyData.getClosePrice().doubleValue())
                .volume(dailyData.getVolume())
                .build();
    }
    
    private McpResponse.TechnicalIndicators calculateAllIndicators(List<DailyData> data) {
        if (data.isEmpty()) {
            return McpResponse.TechnicalIndicators.builder()
                    .rsi(0.0)
                    .sma14(0.0)
                    .sma50(0.0)
                    .ema12(0.0)
                    .ema26(0.0)
                    .macd(0.0)
                    .bollingerUpper(0.0)
                    .bollingerLower(0.0)
                    .stochasticK(0.0)
                    .stochasticD(0.0)
                    .build();
        }
        
        try {
            // Convert to TA4J format
            org.ta4j.core.BarSeries series = new org.ta4j.core.BaseBarSeries("historical_analysis");
            
            for (DailyData dailyData : data) {
                org.ta4j.core.Bar bar = new org.ta4j.core.BaseBar(
                        java.time.Duration.ofDays(1),
                        java.time.ZonedDateTime.of(dailyData.getDate().atStartOfDay(), 
                                               java.time.ZoneId.systemDefault()),
                        dailyData.getOpenPrice(),
                        dailyData.getHighPrice(),
                        dailyData.getLowPrice(),
                        dailyData.getClosePrice(),
                        new java.math.BigDecimal(dailyData.getVolume())
                );
                series.addBar(bar);
            }
            
            // Calculate indicators using TA4J
            org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice = 
                    new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
            
            int endIndex = series.getEndIndex();
            
            // RSI
            org.ta4j.core.indicators.RSIIndicator rsi = new org.ta4j.core.indicators.RSIIndicator(closePrice, 14);
            double rsiValue = rsi.getValue(endIndex).doubleValue();
            
            // Moving Averages
            org.ta4j.core.indicators.SMAIndicator sma14 = new org.ta4j.core.indicators.SMAIndicator(closePrice, 14);
            org.ta4j.core.indicators.SMAIndicator sma50 = new org.ta4j.core.indicators.SMAIndicator(closePrice, 50);
            org.ta4j.core.indicators.EMAIndicator ema12 = new org.ta4j.core.indicators.EMAIndicator(closePrice, 12);
            org.ta4j.core.indicators.EMAIndicator ema26 = new org.ta4j.core.indicators.EMAIndicator(closePrice, 26);
            
            double sma14Value = sma14.getValue(endIndex).doubleValue();
            double sma50Value = series.getBarCount() >= 50 ? sma50.getValue(endIndex).doubleValue() : 0.0;
            double ema12Value = series.getBarCount() >= 12 ? ema12.getValue(endIndex).doubleValue() : 0.0;
            double ema26Value = series.getBarCount() >= 26 ? ema26.getValue(endIndex).doubleValue() : 0.0;
            
            // MACD
            org.ta4j.core.indicators.MACDIndicator macd = new org.ta4j.core.indicators.MACDIndicator(closePrice, 12, 26);
            double macdValue = series.getBarCount() >= 26 ? macd.getValue(endIndex).doubleValue() : 0.0;
            
            // Bollinger Bands
            org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator bbMiddle = 
                    new org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator(sma14);
            org.ta4j.core.indicators.statistics.StandardDeviationIndicator stdDev = 
                    new org.ta4j.core.indicators.statistics.StandardDeviationIndicator(closePrice, 14);
            org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator bbUpper = 
                    new org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator(bbMiddle, stdDev);
            org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator bbLower = 
                    new org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator(bbMiddle, stdDev);
            
            double bollingerUpper = bbUpper.getValue(endIndex).doubleValue();
            double bollingerLower = bbLower.getValue(endIndex).doubleValue();
            
            // Stochastic Oscillator
            org.ta4j.core.indicators.StochasticOscillatorKIndicator stochK = 
                    new org.ta4j.core.indicators.StochasticOscillatorKIndicator(series, 14);
            org.ta4j.core.indicators.StochasticOscillatorDIndicator stochD = 
                    new org.ta4j.core.indicators.StochasticOscillatorDIndicator(stochK);
            
            double stochasticK = stochK.getValue(endIndex).doubleValue();
            double stochasticD = stochD.getValue(endIndex).doubleValue();
            
            return McpResponse.TechnicalIndicators.builder()
                    .rsi(rsiValue)
                    .sma14(sma14Value)
                    .sma50(sma50Value)
                    .ema12(ema12Value)
                    .ema26(ema26Value)
                    .macd(macdValue)
                    .bollingerUpper(bollingerUpper)
                    .bollingerLower(bollingerLower)
                    .stochasticK(stochasticK)
                    .stochasticD(stochasticD)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error calculating technical indicators", e);
            return McpResponse.TechnicalIndicators.builder()
                    .rsi(0.0)
                    .sma14(0.0)
                    .sma50(0.0)
                    .ema12(0.0)
                    .ema26(0.0)
                    .macd(0.0)
                    .bollingerUpper(0.0)
                    .bollingerLower(0.0)
                    .stochasticK(0.0)
                    .stochasticD(0.0)
                    .build();
        }
    }
    
    /**
     * Performs advanced pattern recognition and market sentiment analysis
     */
    public Mono<Map<String, Object>> performAdvancedAnalysis(String symbol, int days) {
        log.info("Performing advanced analysis for symbol: {} with {} days", symbol, days);
        
        return stockDataRepository.findByStockSymbolAndDataType(symbol, "HISTORICAL")
                .switchIfEmpty(fetchHistoricalData(symbol))
                .map(stockData -> {
                    if (stockData.getDailyData().isEmpty()) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", "No historical data available");
                        return errorResult;
                    }
                    
                    List<DailyData> limitedData = stockData.getDailyData().stream()
                            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                            .limit(days)
                            .collect(Collectors.toList());
                    
                    return performPatternAnalysis(limitedData, symbol);
                })
                .onErrorResume(e -> {
                    log.error("Error in advanced analysis for symbol: {}", symbol, e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "Advanced analysis failed: " + e.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * Performs comprehensive pattern recognition analysis
     */
    private Map<String, Object> performPatternAnalysis(List<DailyData> data, String symbol) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            // Convert to TA4J format
            org.ta4j.core.BarSeries series = new org.ta4j.core.BaseBarSeries("pattern_analysis");
            
            for (DailyData dailyData : data) {
                org.ta4j.core.Bar bar = new org.ta4j.core.BaseBar(
                        java.time.Duration.ofDays(1),
                        java.time.ZonedDateTime.of(dailyData.getDate().atStartOfDay(), 
                                               java.time.ZoneId.systemDefault()),
                        dailyData.getOpenPrice(),
                        dailyData.getHighPrice(),
                        dailyData.getLowPrice(),
                        dailyData.getClosePrice(),
                        new java.math.BigDecimal(dailyData.getVolume())
                );
                series.addBar(bar);
            }
            
            // Basic pattern analysis
            analysis.put("symbol", symbol);
            analysis.put("dataPoints", data.size());
            analysis.put("period", data.size() + " days");
            analysis.put("analysisTimestamp", LocalDateTime.now().toString());
            
            // Price patterns
            analysis.put("pricePatterns", analyzePricePatterns(series));
            
            // Volume patterns
            analysis.put("volumePatterns", analyzeVolumePatterns(series));
            
            // Candlestick patterns
            analysis.put("candlestickPatterns", analyzeCandlestickPatterns(series));
            
            // Trend analysis
            analysis.put("trendAnalysis", analyzeTrendStrength(series));
            
            // Support and resistance levels
            analysis.put("supportResistance", findSupportResistanceLevels(series));
            
            // Market sentiment
            analysis.put("marketSentiment", calculateMarketSentiment(series));
            
            // Risk metrics
            analysis.put("riskMetrics", calculateRiskMetrics(series));
            
        } catch (Exception e) {
            log.error("Error in pattern analysis", e);
            analysis.put("error", "Pattern analysis failed: " + e.getMessage());
        }
        
        return analysis;
    }
    
    private Map<String, Object> analyzePricePatterns(org.ta4j.core.BarSeries series) {
        Map<String, Object> patterns = new HashMap<>();
        
        try {
            org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice = 
                    new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
            
            // Moving average convergence/divergence
            patterns.put("macdTrend", analyzeMACDTrend(closePrice));
            
            // Price momentum
            patterns.put("momentum", calculatePriceMomentum(closePrice));
            
            // Volatility patterns
            patterns.put("volatility", calculateVolatility(closePrice));
            
            // Gap analysis
            patterns.put("gaps", analyzeGaps(series));
            
        } catch (Exception e) {
            patterns.put("error", "Price pattern analysis failed: " + e.getMessage());
        }
        
        return patterns;
    }
    
    private Map<String, Object> analyzeVolumePatterns(org.ta4j.core.BarSeries series) {
        Map<String, Object> patterns = new HashMap<>();
        
        try {
            org.ta4j.core.indicators.helpers.VolumeIndicator volume = 
                    new org.ta4j.core.indicators.helpers.VolumeIndicator(series);
            
            // Volume trend
            patterns.put("volumeTrend", analyzeVolumeTrend(volume));
            
            // Volume-price relationship
            patterns.put("volumePriceRelation", analyzeVolumePriceRelation(series));
            
        } catch (Exception e) {
            patterns.put("error", "Volume pattern analysis failed: " + e.getMessage());
        }
        
        return patterns;
    }
    
    private Map<String, Object> analyzeCandlestickPatterns(org.ta4j.core.BarSeries series) {
        Map<String, Object> patterns = new HashMap<>();
        
        try {
            // Analyze recent candlestick patterns
            patterns.put("dojiPatterns", findDojiPatterns(series));
            patterns.put("hammerPatterns", findHammerPatterns(series));
            patterns.put("engulfingPatterns", findEngulfingPatterns(series));
            
        } catch (Exception e) {
            patterns.put("error", "Candlestick pattern analysis failed: " + e.getMessage());
        }
        
        return patterns;
    }
    
    private Map<String, Object> analyzeTrendStrength(org.ta4j.core.BarSeries series) {
        Map<String, Object> trend = new HashMap<>();
        
        try {
            org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice = 
                    new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
            
            // ADX for trend strength
            org.ta4j.core.indicators.adx.ADXIndicator adx = 
                    new org.ta4j.core.indicators.adx.ADXIndicator(series, 14);
            
            double adxValue = adx.getValue(series.getEndIndex()).doubleValue();
            trend.put("adx", adxValue);
            trend.put("trendStrength", interpretADX(adxValue));
            
            // Moving average slopes
            trend.put("maSlopes", calculateMASlopes(closePrice));
            
        } catch (Exception e) {
            trend.put("error", "Trend analysis failed: " + e.getMessage());
        }
        
        return trend;
    }
    
    private Map<String, Object> findSupportResistanceLevels(org.ta4j.core.BarSeries series) {
        Map<String, Object> levels = new HashMap<>();
        
        try {
            List<Double> highs = new ArrayList<>();
            List<Double> lows = new ArrayList<>();
            
            for (int i = 0; i < series.getBarCount(); i++) {
                highs.add(series.getBar(i).getHighPrice().doubleValue());
                lows.add(series.getBar(i).getLowPrice().doubleValue());
            }
            
            // Find recent support and resistance levels
            levels.put("resistance", findResistanceLevels(highs));
            levels.put("support", findSupportLevels(lows));
            
        } catch (Exception e) {
            levels.put("error", "Support/Resistance analysis failed: " + e.getMessage());
        }
        
        return levels;
    }
    
    private Map<String, Object> calculateMarketSentiment(org.ta4j.core.BarSeries series) {
        Map<String, Object> sentiment = new HashMap<>();
        
        try {
            // Calculate sentiment based on multiple indicators
            int bullishSignals = 0;
            int bearishSignals = 0;
            int totalSignals = 0;
            
            org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice = 
                    new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
            
            // RSI sentiment
            org.ta4j.core.indicators.RSIIndicator rsi = new org.ta4j.core.indicators.RSIIndicator(closePrice, 14);
            double rsiValue = rsi.getValue(series.getEndIndex()).doubleValue();
            if (rsiValue > 50) bullishSignals++;
            else bearishSignals++;
            totalSignals++;
            
            // MACD sentiment
            org.ta4j.core.indicators.MACDIndicator macd = new org.ta4j.core.indicators.MACDIndicator(closePrice, 12, 26);
            if (series.getBarCount() >= 26) {
                double macdValue = macd.getValue(series.getEndIndex()).doubleValue();
                if (macdValue > 0) bullishSignals++;
                else bearishSignals++;
                totalSignals++;
            }
            
            // Calculate overall sentiment
            double sentimentScore = (double) bullishSignals / totalSignals;
            sentiment.put("bullishSignals", bullishSignals);
            sentiment.put("bearishSignals", bearishSignals);
            sentiment.put("totalSignals", totalSignals);
            sentiment.put("sentimentScore", sentimentScore);
            sentiment.put("sentiment", interpretSentiment(sentimentScore));
            
        } catch (Exception e) {
            sentiment.put("error", "Market sentiment analysis failed: " + e.getMessage());
        }
        
        return sentiment;
    }
    
    private Map<String, Object> calculateRiskMetrics(org.ta4j.core.BarSeries series) {
        Map<String, Object> risk = new HashMap<>();
        
        try {
            org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice = 
                    new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
            
            // Standard deviation (volatility)
            org.ta4j.core.indicators.statistics.StandardDeviationIndicator stdDev = 
                    new org.ta4j.core.indicators.statistics.StandardDeviationIndicator(closePrice, 20);
            double volatility = stdDev.getValue(series.getEndIndex()).doubleValue();
            
            risk.put("volatility", volatility);
            risk.put("riskLevel", interpretRisk(volatility));
            
            // Calculate maximum drawdown
            risk.put("maxDrawdown", calculateMaxDrawdown(series));
            
        } catch (Exception e) {
            risk.put("error", "Risk metrics calculation failed: " + e.getMessage());
        }
        
        return risk;
    }
    
    // Helper methods for pattern analysis
    private String analyzeMACDTrend(org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice) {
        try {
            org.ta4j.core.indicators.MACDIndicator macd = new org.ta4j.core.indicators.MACDIndicator(closePrice, 12, 26);
            int endIndex = closePrice.getBarSeries().getEndIndex();
            
            if (closePrice.getBarSeries().getBarCount() >= 26) {
                double currentMACD = macd.getValue(endIndex).doubleValue();
                double previousMACD = macd.getValue(endIndex - 1).doubleValue();
                
                if (currentMACD > previousMACD && currentMACD > 0) {
                    return "STRONG_BULLISH";
                } else if (currentMACD > previousMACD && currentMACD < 0) {
                    return "BULLISH_RECOVERY";
                } else if (currentMACD < previousMACD && currentMACD > 0) {
                    return "BEARISH_CORRECTION";
                } else {
                    return "STRONG_BEARISH";
                }
            }
            return "INSUFFICIENT_DATA";
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private String calculatePriceMomentum(org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice) {
        try {
            // Calculate simple momentum manually
            int endIndex = closePrice.getBarSeries().getEndIndex();
            if (endIndex < 10) return "INSUFFICIENT_DATA";
            
            double currentPrice = closePrice.getValue(endIndex).doubleValue();
            double pastPrice = closePrice.getValue(endIndex - 10).doubleValue();
            double momentum = currentPrice / pastPrice;
            
            if (momentum > 1.05) return "STRONG_POSITIVE";
            else if (momentum > 1.02) return "POSITIVE";
            else if (momentum > 0.98) return "NEUTRAL";
            else if (momentum > 0.95) return "NEGATIVE";
            else return "STRONG_NEGATIVE";
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private String calculateVolatility(org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice) {
        try {
            org.ta4j.core.indicators.statistics.StandardDeviationIndicator stdDev = 
                    new org.ta4j.core.indicators.statistics.StandardDeviationIndicator(closePrice, 20);
            double volatility = stdDev.getValue(closePrice.getBarSeries().getEndIndex()).doubleValue();
            
            if (volatility > 0.05) return "HIGH";
            else if (volatility > 0.03) return "MODERATE";
            else return "LOW";
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private List<String> analyzeGaps(org.ta4j.core.BarSeries series) {
        List<String> gaps = new ArrayList<>();
        
        try {
            for (int i = 1; i < Math.min(series.getBarCount(), 10); i++) {
                double prevClose = series.getBar(i - 1).getClosePrice().doubleValue();
                double currentOpen = series.getBar(i).getOpenPrice().doubleValue();
                double gapPercent = ((currentOpen - prevClose) / prevClose) * 100;
                
                if (Math.abs(gapPercent) > 2.0) {
                    gaps.add(String.format("Gap %.2f%% on %s", gapPercent, series.getBar(i).getEndTime().toLocalDate()));
                }
            }
        } catch (Exception e) {
            gaps.add("Error analyzing gaps: " + e.getMessage());
        }
        
        return gaps;
    }
    
    private String analyzeVolumeTrend(org.ta4j.core.indicators.helpers.VolumeIndicator volume) {
        try {
            org.ta4j.core.indicators.SMAIndicator volumeSMA = new org.ta4j.core.indicators.SMAIndicator(volume, 10);
            int endIndex = volume.getBarSeries().getEndIndex();
            
            double currentVolume = volume.getValue(endIndex).doubleValue();
            double avgVolume = volumeSMA.getValue(endIndex).doubleValue();
            
            if (currentVolume > avgVolume * 1.5) return "HIGH_VOLUME";
            else if (currentVolume > avgVolume * 1.2) return "ABOVE_AVERAGE";
            else if (currentVolume < avgVolume * 0.8) return "BELOW_AVERAGE";
            else return "AVERAGE";
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private String analyzeVolumePriceRelation(org.ta4j.core.BarSeries series) {
        try {
            int endIndex = series.getEndIndex();
            if (endIndex < 1) return "INSUFFICIENT_DATA";
            
            double priceChange = series.getBar(endIndex).getClosePrice().doubleValue() - 
                               series.getBar(endIndex - 1).getClosePrice().doubleValue();
            double volumeChange = series.getBar(endIndex).getVolume().doubleValue() - 
                                series.getBar(endIndex - 1).getVolume().doubleValue();
            
            if (priceChange > 0 && volumeChange > 0) return "BULLISH_CONFIRMATION";
            else if (priceChange < 0 && volumeChange > 0) return "BEARISH_CONFIRMATION";
            else if (priceChange > 0 && volumeChange < 0) return "WEAK_BULLISH";
            else if (priceChange < 0 && volumeChange < 0) return "WEAK_BEARISH";
            else return "NEUTRAL";
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private List<String> findDojiPatterns(org.ta4j.core.BarSeries series) {
        List<String> patterns = new ArrayList<>();
        
        try {
            for (int i = Math.max(0, series.getBarCount() - 5); i < series.getBarCount(); i++) {
                org.ta4j.core.Bar bar = series.getBar(i);
                double bodySize = Math.abs(bar.getClosePrice().doubleValue() - bar.getOpenPrice().doubleValue());
                double totalRange = bar.getHighPrice().doubleValue() - bar.getLowPrice().doubleValue();
                
                if (bodySize < totalRange * 0.1) {
                    patterns.add(String.format("Doji pattern on %s", bar.getEndTime().toLocalDate()));
                }
            }
        } catch (Exception e) {
            patterns.add("Error finding Doji patterns: " + e.getMessage());
        }
        
        return patterns;
    }
    
    private List<String> findHammerPatterns(org.ta4j.core.BarSeries series) {
        List<String> patterns = new ArrayList<>();
        
        try {
            for (int i = Math.max(0, series.getBarCount() - 5); i < series.getBarCount(); i++) {
                org.ta4j.core.Bar bar = series.getBar(i);
                double open = bar.getOpenPrice().doubleValue();
                double close = bar.getClosePrice().doubleValue();
                double high = bar.getHighPrice().doubleValue();
                double low = bar.getLowPrice().doubleValue();
                
                double bodySize = Math.abs(close - open);
                double lowerShadow = Math.min(open, close) - low;
                double upperShadow = high - Math.max(open, close);
                
                if (lowerShadow > bodySize * 2 && upperShadow < bodySize * 0.5) {
                    patterns.add(String.format("Hammer pattern on %s", bar.getEndTime().toLocalDate()));
                }
            }
        } catch (Exception e) {
            patterns.add("Error finding Hammer patterns: " + e.getMessage());
        }
        
        return patterns;
    }
    
    private List<String> findEngulfingPatterns(org.ta4j.core.BarSeries series) {
        List<String> patterns = new ArrayList<>();
        
        try {
            for (int i = Math.max(1, series.getBarCount() - 5); i < series.getBarCount(); i++) {
                org.ta4j.core.Bar current = series.getBar(i);
                org.ta4j.core.Bar previous = series.getBar(i - 1);
                
                boolean bullishEngulfing = previous.getClosePrice().doubleValue() < previous.getOpenPrice().doubleValue() &&
                                         current.getClosePrice().doubleValue() > current.getOpenPrice().doubleValue() &&
                                         current.getOpenPrice().doubleValue() < previous.getClosePrice().doubleValue() &&
                                         current.getClosePrice().doubleValue() > previous.getOpenPrice().doubleValue();
                
                boolean bearishEngulfing = previous.getClosePrice().doubleValue() > previous.getOpenPrice().doubleValue() &&
                                         current.getClosePrice().doubleValue() < current.getOpenPrice().doubleValue() &&
                                         current.getOpenPrice().doubleValue() > previous.getClosePrice().doubleValue() &&
                                         current.getClosePrice().doubleValue() < previous.getOpenPrice().doubleValue();
                
                if (bullishEngulfing) {
                    patterns.add(String.format("Bullish Engulfing on %s", current.getEndTime().toLocalDate()));
                } else if (bearishEngulfing) {
                    patterns.add(String.format("Bearish Engulfing on %s", current.getEndTime().toLocalDate()));
                }
            }
        } catch (Exception e) {
            patterns.add("Error finding Engulfing patterns: " + e.getMessage());
        }
        
        return patterns;
    }
    
    private String interpretADX(double adxValue) {
        if (adxValue > 50) return "VERY_STRONG_TREND";
        else if (adxValue > 25) return "STRONG_TREND";
        else if (adxValue > 20) return "MODERATE_TREND";
        else return "WEAK_TREND";
    }
    
    private Map<String, String> calculateMASlopes(org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice) {
        Map<String, String> slopes = new HashMap<>();
        
        try {
            org.ta4j.core.indicators.SMAIndicator sma20 = new org.ta4j.core.indicators.SMAIndicator(closePrice, 20);
            org.ta4j.core.indicators.SMAIndicator sma50 = new org.ta4j.core.indicators.SMAIndicator(closePrice, 50);
            
            int endIndex = closePrice.getBarSeries().getEndIndex();
            
            if (endIndex >= 20) {
                double sma20Current = sma20.getValue(endIndex).doubleValue();
                double sma20Previous = sma20.getValue(endIndex - 5).doubleValue();
                slopes.put("sma20", sma20Current > sma20Previous ? "RISING" : "FALLING");
            }
            
            if (endIndex >= 50) {
                double sma50Current = sma50.getValue(endIndex).doubleValue();
                double sma50Previous = sma50.getValue(endIndex - 5).doubleValue();
                slopes.put("sma50", sma50Current > sma50Previous ? "RISING" : "FALLING");
            }
        } catch (Exception e) {
            slopes.put("error", "Error calculating MA slopes: " + e.getMessage());
        }
        
        return slopes;
    }
    
    private List<Double> findResistanceLevels(List<Double> highs) {
        List<Double> levels = new ArrayList<>();
        
        try {
            highs.sort((a, b) -> Double.compare(b, a));
            
            for (int i = 0; i < Math.min(3, highs.size()); i++) {
                levels.add(highs.get(i));
            }
        } catch (Exception e) {
            log.error("Error finding resistance levels", e);
        }
        
        return levels;
    }
    
    private List<Double> findSupportLevels(List<Double> lows) {
        List<Double> levels = new ArrayList<>();
        
        try {
            lows.sort(Double::compareTo);
            
            for (int i = 0; i < Math.min(3, lows.size()); i++) {
                levels.add(lows.get(i));
            }
        } catch (Exception e) {
            log.error("Error finding support levels", e);
        }
        
        return levels;
    }
    
    private String interpretSentiment(double sentimentScore) {
        if (sentimentScore > 0.7) return "VERY_BULLISH";
        else if (sentimentScore > 0.6) return "BULLISH";
        else if (sentimentScore > 0.4) return "NEUTRAL";
        else if (sentimentScore > 0.3) return "BEARISH";
        else return "VERY_BEARISH";
    }
    
    private String interpretRisk(double volatility) {
        if (volatility > 0.05) return "HIGH_RISK";
        else if (volatility > 0.03) return "MODERATE_RISK";
        else return "LOW_RISK";
    }
    
    private double calculateMaxDrawdown(org.ta4j.core.BarSeries series) {
        double maxDrawdown = 0.0;
        
        try {
            double peak = series.getBar(0).getClosePrice().doubleValue();
            
            for (int i = 1; i < series.getBarCount(); i++) {
                double current = series.getBar(i).getClosePrice().doubleValue();
                
                if (current > peak) {
                    peak = current;
                } else {
                    double drawdown = (peak - current) / peak;
                    maxDrawdown = Math.max(maxDrawdown, drawdown);
                }
            }
        } catch (Exception e) {
            log.error("Error calculating max drawdown", e);
        }
        
        return maxDrawdown * 100; // Return as percentage
    }
}
