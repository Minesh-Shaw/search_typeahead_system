package com.minesh.search_typeahead_system.repository;

import com.minesh.search_typeahead_system.model.SearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Open/Closed Principle: Extending JPA functionality without modifying it.
public interface SearchQueryRepo extends JpaRepository<SearchQuery, String> {
    List<SearchQuery> findTop100ByQueryStartingWithOrderBySearchCountDesc(String prefix);
    List<SearchQuery> findTop100ByOrderBySearchCountDesc();
}