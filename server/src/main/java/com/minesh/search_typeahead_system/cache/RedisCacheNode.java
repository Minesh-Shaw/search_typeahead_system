package com.minesh.search_typeahead_system.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.List;

// Redis Implementation
public class RedisCacheNode implements CacheNode {
    private final String nodeId;
    private final StringRedisTemplate redisTemplate;

    public RedisCacheNode(String nodeId, StringRedisTemplate redisTemplate) {
        this.nodeId = nodeId;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getNodeId() { return nodeId; }

    @Override
    public void putSuggestions(String prefix, List<String> suggestions) {
        String key = "node:" + nodeId + ":prefix:" + prefix;
        // Store as a list and set expiry (Caching Expectation Rubric)
        redisTemplate.opsForList().rightPushAll(key, suggestions);
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        String key = "node:" + nodeId + ":prefix:" + prefix;
        return redisTemplate.opsForList().range(key, 0, -1);
    }
}