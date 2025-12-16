package com.pms.analytics.externalRedis;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisPriceCache {

    private final RedisTemplate<String, Object> redis;
    private static final String PRICE_KEY = "prices";

    public void updatePrice(String symbol, BigDecimal price) {
        redis.opsForHash().put(PRICE_KEY, symbol, price);
    }

    public BigDecimal getPrice(String symbol) {
        Object value = redis.opsForHash().get(PRICE_KEY, symbol);
        return value != null ? new BigDecimal(value.toString()) : null;
    }

    public Map<String, BigDecimal> getAllPrices() {
        Map<Object, Object> entries = redis.opsForHash().entries(PRICE_KEY);
        return entries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> new BigDecimal(e.getValue().toString())
                ));
    }
}
