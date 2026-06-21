package com.minesh.search_typeahead_system.config;

import com.minesh.search_typeahead_system.cache.ConsistentHashRouter;
import com.minesh.search_typeahead_system.cache.MemoryCacheNode;
import com.minesh.search_typeahead_system.cache.RedisCacheNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AppConfig {

    // Helper method to dynamically generate physical Redis connections
    private StringRedisTemplate createRedisTemplate(String host, int port) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet(); // Initialize the connection
        
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ConsistentHashRouter cacheRouter(
            @Value("${app.cache.type}") String cacheType,
            @Value("${app.redis.node1.host}") String host1, @Value("${app.redis.node1.port}") int port1,
            @Value("${app.redis.node2.host}") String host2, @Value("${app.redis.node2.port}") int port2,
            @Value("${app.redis.node3.host}") String host3, @Value("${app.redis.node3.port}") int port3) {

        ConsistentHashRouter router = new ConsistentHashRouter();

        if ("redis".equalsIgnoreCase(cacheType)) {
            // Physical mapping to 3 distinct Redis containers
            router.addNode(new RedisCacheNode("redis-node-1", createRedisTemplate(host1, port1)), 3);
            router.addNode(new RedisCacheNode("redis-node-2", createRedisTemplate(host2, port2)), 3);
            router.addNode(new RedisCacheNode("redis-node-3", createRedisTemplate(host3, port3)), 3);
            System.out.println("Initialized Physically Distributed Redis Cache (3 Nodes).");
        } else {
            router.addNode(new MemoryCacheNode("mem-node-1"), 50);
            router.addNode(new MemoryCacheNode("mem-node-2"), 50);
            router.addNode(new MemoryCacheNode("mem-node-3"), 50);
            System.out.println("Initialized In-Memory Cache (3 Nodes).");
        }

        return router;
    }
}