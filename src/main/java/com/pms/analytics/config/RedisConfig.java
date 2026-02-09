package com.pms.analytics.config;

import java.time.Duration;
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
    private Duration redisTimeoutMs;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;


    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinelMaster);

        for (String node : sentinelNodes) {
            String[] parts = node.split(":");
            config.sentinel(new RedisNode(parts[0], Integer.parseInt(parts[1])));
        }

        if(redisPassword != null && !redisPassword.isEmpty()){
            config.setPassword(redisPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.setTimeout(redisTimeoutMs.toMillis());
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
