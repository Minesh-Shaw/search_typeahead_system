package com.minesh.search_typeahead_system.bootstrap;

import com.minesh.search_typeahead_system.model.SearchQuery;
import com.minesh.search_typeahead_system.repository.SearchQueryRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SearchQueryRepo repo;

    public DataSeeder(SearchQueryRepo repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Check if database is already seeded to prevent duplicate work on restart
        if (repo.count() > 0) {
            System.out.println("Database already contains data. Skipping seeding.");
            return;
        }

        System.out.println("Starting data ingestion from dataset.csv...");
        long startTime = System.currentTimeMillis();

        // 2. Read the file from the packaged JAR resources
        ClassPathResource resource = new ClassPathResource("dataset.csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            List<SearchQuery> batch = new ArrayList<>();
            int batchSize = 1000; // Optimal for cloud database inserts

            while ((line = reader.readLine()) != null) {
                // Skip the CSV header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] data = line.split(",");
                if (data.length == 2) {
                    String query = data[0].trim().toLowerCase();
                    long count = Long.parseLong(data[1].trim());

                    // We initialize lastSearchedAt to now for the base dataset
                    batch.add(new SearchQuery(query, count, Instant.now()));

                    // Flush the batch to the database
                    if (batch.size() >= batchSize) {
                        repo.saveAll(batch);
                        batch.clear();
                    }
                }
            }

            // Flush any remaining records
            if (!batch.isEmpty()) {
                repo.saveAll(batch);
            }

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Data ingestion completed successfully in " + duration + "ms.");
        } catch (Exception e) {
            System.err.println("Failed to seed database: " + e.getMessage());
        }
    }
}