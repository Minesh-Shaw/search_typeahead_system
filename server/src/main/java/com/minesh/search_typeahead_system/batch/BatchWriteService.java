package com.minesh.search_typeahead_system.batch;

import com.minesh.search_typeahead_system.model.SearchQuery;
import com.minesh.search_typeahead_system.repository.SearchQueryRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class BatchWriteService {

    private final SearchQueryRepo repo;
    
    // Using volatile ensures the swapped reference is immediately visible to all threads
    private volatile Map<String, Long> writeBuffer = new ConcurrentHashMap<>();

    public BatchWriteService(SearchQueryRepo repo) { 
        this.repo = repo; 
    }

    public void registerSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        // Increment count instantly in the active buffer
        writeBuffer.merge(query.trim().toLowerCase(), 1L, Long::sum);
    }

    @Transactional // Forces the entire batch flush to execute as a single atomic DB operation
    @Scheduled(fixedDelayString = "${app.batch.flush-interval-ms}")
    public void flushToDatabase() {
        if (writeBuffer.isEmpty()) return;

        // 1. ATOMIC SWAP: Grab the current data and instantly replace the active buffer.
        Map<String, Long> snapshot = writeBuffer;
        writeBuffer = new ConcurrentHashMap<>();

        // 2. OPTIMIZATION: Fetch all existing database records in a single query (prevents N+1 database hits)
        List<SearchQuery> existingQueries = repo.findAllById(snapshot.keySet());
        Map<String, SearchQuery> existingMap = existingQueries.stream()
                .collect(Collectors.toMap(SearchQuery::getQuery, q -> q));

        List<SearchQuery> toSave = new ArrayList<>();

        // 3. Process the snapshot safely
        snapshot.forEach((queryStr, count) -> {
            SearchQuery sq = existingMap.getOrDefault(queryStr, new SearchQuery(queryStr, 0, Instant.now()));
            
            // Add the aggregated buffer count to the historical database count
            sq.setSearchCount(sq.getSearchCount() + count);
            sq.setLastSearchedAt(Instant.now());
            
            toSave.add(sq);
        });
        
        // 4. BULK SAVE: Execute a single batch UPSERT to PostgreSQL
        repo.saveAll(toSave);
        
        System.out.println("Flushed " + snapshot.size() + " unique queries to PostgreSQL.");
    }
}