package com.pms.analytics.externalRedis;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisPriceCache {

    private final RedisTemplate<String, Object> redis;

    @Value("${app.redis.price-key:prices}")
    private String priceKey;

    public void updatePrice(String symbol, BigDecimal price) {
        redis.opsForHash().put(priceKey, symbol, price);
    }

    public BigDecimal getPrice(String symbol) {
        Object value = redis.opsForHash().get(priceKey, symbol);
        return value != null ? new BigDecimal(value.toString()) : null;
    }

    public Map<String, BigDecimal> getAllPrices() {
        Map<Object, Object> entries = redis.opsForHash().entries(priceKey);
        return entries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> new BigDecimal(e.getValue().toString())
                ));
    }
}
