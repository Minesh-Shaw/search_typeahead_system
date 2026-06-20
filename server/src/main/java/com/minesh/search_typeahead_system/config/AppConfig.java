package com.minesh.search_typeahead_system.config;

import com.minesh.search_typeahead_system.cache.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AppConfig {

    @Bean
    public ConsistentHashRouter cacheRouter(@Value("${app.cache.type}") String cacheType,
                                            StringRedisTemplate redisTemplate) {
        ConsistentHashRouter router = new ConsistentHashRouter();

        // Easily switchable based on config property
        if ("redis".equalsIgnoreCase(cacheType)) {
            router.addNode(new RedisCacheNode("redis-node-1", redisTemplate), 3);
            router.addNode(new RedisCacheNode("redis-node-2", redisTemplate), 3);
            router.addNode(new RedisCacheNode("redis-node-3", redisTemplate), 3);
        } else {
            // Fallback to memory if Redis fails/is turned off
            router.addNode(new MemoryCacheNode("mem-node-1"), 3);
            router.addNode(new MemoryCacheNode("mem-node-2"), 3);
        }
        return router;
    }
}