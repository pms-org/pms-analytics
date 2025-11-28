package com.pms.analytics.controller;

import com.pms.analytics.service.ExternalPriceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/price")
@RequiredArgsConstructor
public class ExternalPriceController {


    private final ExternalPriceClient externalPriceClient;


    @GetMapping("/{symbol}")
    public ResponseEntity<?> getPrice(@PathVariable String symbol) {
        try {
            BigDecimal price = externalPriceClient.getCurrentPrice(symbol);
            return ResponseEntity.ok(price);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

}
