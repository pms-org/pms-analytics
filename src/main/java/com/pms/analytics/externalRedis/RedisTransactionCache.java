// package com.pms.analytics.externalRedis;

// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.stereotype.Service;

// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// public class RedisTransactionCache {

//     private final StringRedisTemplate redisTemplate;
//     private static final String PREFIX = "transaction:";

//     public boolean isDuplicate(String transactionId) {
//         String key = PREFIX + transactionId;
//         Boolean exists = redisTemplate.hasKey(key);
//         return exists != null && exists;
//     }

//     public void markProcessed(String transactionId) {
//         String key = PREFIX + transactionId;
//         redisTemplate.opsForValue().set(key, "processed");
//     }
// }

package com.pms.analytics.externalRedis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisTransactionCache {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.redis.transaction-key:transaction:}")
    private String prefix;

    public boolean isDuplicate(String transactionId) {
        String key = prefix + transactionId;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    public void markProcessed(String transactionId) {
        String key = prefix + transactionId;
        redisTemplate.opsForValue().set(key, "processed");
    }
}
