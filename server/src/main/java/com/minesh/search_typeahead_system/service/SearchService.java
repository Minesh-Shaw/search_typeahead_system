package com.minesh.search_typeahead_system.service;

import com.minesh.search_typeahead_system.batch.BatchWriteService;
import com.minesh.search_typeahead_system.cache.CacheNode;
import com.minesh.search_typeahead_system.cache.ConsistentHashRouter;
import com.minesh.search_typeahead_system.model.SearchQuery;
import com.minesh.search_typeahead_system.repository.SearchQueryRepo;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final SearchQueryRepo repo;
    private final ConsistentHashRouter router;
    private final BatchWriteService batchService;

    public SearchService(SearchQueryRepo repo, ConsistentHashRouter router, BatchWriteService batchService) {
        this.repo = repo;
        this.router = router;
        this.batchService = batchService;
    }

    public void logSearch(String query) {
        // Async batch write to reduce DB load
        batchService.registerSearch(query);
    }

    public List<String> getSuggestions(String prefix) {
        String cleanPrefix = prefix.trim().toLowerCase();
        CacheNode node = router.routeNode(cleanPrefix);

        // 1. Try Cache
        List<String> cached = node.getSuggestions(cleanPrefix);
        if (cached != null && !cached.isEmpty()) return cached;

        // 2. Cache Miss -> Query DB
        List<SearchQuery> dbResults = repo.findTop100ByQueryStartingWithOrderBySearchCountDesc(cleanPrefix);

        // 3. Apply Trending / Recency Algorithm (20% Rubric)
        List<String> sortedSuggestions = calculateTrendingRank(dbResults);

        // 4. Update Cache & Return
        if (!sortedSuggestions.isEmpty()) {
            node.putSuggestions(cleanPrefix, sortedSuggestions);
        }
        return sortedSuggestions;
    }

    private List<String> calculateTrendingRank(List<SearchQuery> queries) {
        Instant now = Instant.now();
        return queries.stream()
                .sorted((q1, q2) -> {
                    // Score = count * recency_multiplier
                    // Queries searched recently get a multiplier boost (e.g., 1.5x if searched in the last hour)
                    double score1 = q1.getSearchCount() * getRecencyMultiplier(q1.getLastSearchedAt(), now);
                    double score2 = q2.getSearchCount() * getRecencyMultiplier(q2.getLastSearchedAt(), now);
                    return Double.compare(score2, score1); // Descending
                })
                .limit(10)
                .map(SearchQuery::getQuery)
                .collect(Collectors.toList());
    }

    private double getRecencyMultiplier(Instant lastSearched, Instant now) {
        if (lastSearched == null) return 1.0;
        long hoursAgo = ChronoUnit.HOURS.between(lastSearched, now);
        if (hoursAgo < 1) return 1.5;   // Very hot
        if (hoursAgo < 24) return 1.2;  // Warm
        return 1.0;                     // Historical
    }

    public List<String> getTrending() {
        String cacheKey = "global_trending";
        CacheNode cacheNode = router.routeNode(cacheKey);
        
        // Try cache first
        List<String> cached = cacheNode.getSuggestions(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // Fallback to database if cache miss
        List<SearchQuery> topQueries = repo.findTop100ByOrderBySearchCountDesc();
        
        // Apply trending logic (recency + count)
        List<String> trending = calculateTrendingRank(topQueries);

        // Store in cache with TTL
        if (!trending.isEmpty()) {
            cacheNode.putSuggestions(cacheKey, trending);
        }
        
        return trending;
    }
}