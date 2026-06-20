package com.minesh.search_typeahead_system.batch;

import com.minesh.search_typeahead_system.model.SearchQuery;
import com.minesh.search_typeahead_system.repository.SearchQueryRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BatchWriteService {

    private final SearchQueryRepo repo;
    // Buffer to hold queries in memory temporarily
    private final Map<String, Long> writeBuffer = new ConcurrentHashMap<>();

    public BatchWriteService(SearchQueryRepo repo) { this.repo = repo; }

    public void registerSearch(String query) {
        // Increment count in buffer instantly without hitting DB
        writeBuffer.merge(query.trim().toLowerCase(), 1L, Long::sum);
    }

    // Flushes buffer to Postgres every X seconds
    @Scheduled(fixedDelayString = "${app.batch.flush-interval-ms}")
    public synchronized void flushToDatabase() {
        if (writeBuffer.isEmpty()) return;

        writeBuffer.forEach((queryStr, count) -> {
            SearchQuery sq = repo.findById(queryStr).orElse(new SearchQuery(queryStr, 0, Instant.now()));
            sq.setSearchCount(sq.getSearchCount() + count);
            sq.setLastSearchedAt(Instant.now());
            repo.save(sq);
        });

        writeBuffer.clear(); // Clear buffer after successful flush
    }
}