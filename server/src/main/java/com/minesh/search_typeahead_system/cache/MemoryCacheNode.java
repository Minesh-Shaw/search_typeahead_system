package com.minesh.search_typeahead_system.cache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCacheNode implements CacheNode {

    private final String nodeId;
    // Thread-safe map to store our prefixes and their expiring results
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public MemoryCacheNode(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public void putSuggestions(String prefix, List<String> suggestions) {
        // Match the Redis expiry time
        long ttlMinutes = 10;
        Instant expiryTime = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
        cache.put(prefix, new CacheEntry(suggestions, expiryTime));
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        CacheEntry entry = cache.get(prefix);

        // Cache Miss
        if (entry == null) {
            return null;
        }

        // Cache Expiry / Invalidation Check
        if (Instant.now().isAfter(entry.expiryTime)) {
            cache.remove(prefix); // Clean up stale data
            return null;
        }

        // Cache Hit
        return entry.suggestions;
    }

    /**
         * Internal wrapper to hold the suggestions and their expiration timestamp.
         */
        private record CacheEntry(List<String> suggestions, Instant expiryTime) {
    }
}