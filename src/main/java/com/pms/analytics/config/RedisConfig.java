// package com.pms.analytics.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.connection.RedisSentinelConfiguration;
// import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.StringRedisSerializer;

// @Configuration
// public class RedisConfig {

//     @Bean
//     public LettuceConnectionFactory redisConnectionFactory() {
//         RedisSentinelConfiguration config
//                 = new RedisSentinelConfiguration()
//                         .master("mymaster")
//                         .sentinel("sentinel-1", 26379)
//                         .sentinel("sentinel-2", 26379)
//                         .sentinel("sentinel-3", 26379);

//         return new LettuceConnectionFactory(config);
//     }

//     @Bean
//     public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//         RedisTemplate<String, Object> template = new RedisTemplate<>();
//         template.setConnectionFactory(factory);
//         template.setKeySerializer(new StringRedisSerializer());
//         template.setHashKeySerializer(new StringRedisSerializer());
//         template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//         template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
//         template.afterPropertiesSet();
//         return template;
//     }
// }


package com.pms.analytics.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.sentinel.master}")
    private String sentinelMaster;

    @Value("#{'${spring.data.redis.sentinel.nodes}'.split(',')}")
    private List<String> sentinelNodes;

    @Value("${spring.data.redis.timeout}")
    private long redisTimeoutMs;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinelMaster);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        for (String node : sentinelNodes) {
            String[] parts = node.split(":");
            config.sentinel(new RedisNode(parts[0], Integer.parseInt(parts[1])));
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.setTimeout(redisTimeoutMs);
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
