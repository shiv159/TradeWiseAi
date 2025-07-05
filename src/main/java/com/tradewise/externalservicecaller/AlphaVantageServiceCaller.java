package com.tradewise.externalservicecaller;

import com.tradewise.configs.TradeWiseProps;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class AlphaVantageServiceCaller {

    private final WebClient.Builder webClientBuilder; 
    private final TradeWiseProps tradeWiseProps;

    public AlphaVantageServiceCaller(WebClient.Builder webClientBuilder, TradeWiseProps tradeWiseProps) {
        this.webClientBuilder = webClientBuilder;
        this.tradeWiseProps = tradeWiseProps;
    }

    public Mono<String> getCurrentPrice(String symbol) {
        String url = buildUrl("GLOBAL_QUOTE", symbol);
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getHistoricalPrice(String symbol) {
        String url = buildUrl("TIME_SERIES_DAILY", symbol);
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
    }

    private String buildUrl(String function, String symbol) {
        return String.format("%s?function=%s&symbol=%s%s&apikey=%s",
                tradeWiseProps.getAlphaVantage().getBaseUrl(),
                function,
                symbol,
                tradeWiseProps.getAlphaVantage().getSymbolSuffix(),
                tradeWiseProps.getAlphaVantage().getApiKey());
    }
}
