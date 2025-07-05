package com.tradewise.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tradewise.service.TradeWiseService;

import reactor.core.publisher.Mono;

@RestController()
@RequestMapping("api/v1")
public class TradeWiseController {

    private TradeWiseService tradeWiseService;

    public TradeWiseController(TradeWiseService tradeWiseService) {
        this.tradeWiseService = tradeWiseService;
    }

    

    @GetMapping("/getCurrentPrice")
    public Mono<String> getCurrentPrice(@RequestParam     (value = "symbol", required = true) String symbol) {
        // This method would typically call a service to get the current price
        // For now, we return a static value for demonstration purposes
        return tradeWiseService.getCurrentPrice(symbol);
    }

    @GetMapping("/historicalPrice")
    public Mono<String> getHistoricalPrice(@RequestParam(value = "symbol", required = true) String symbol) {
        // This method would typically call a service to get the historical price
        // For now, we return a static value for demonstration purposes
        return tradeWiseService.getHistoricalPrice(symbol);
    }
    
}
