package com.minesh.search_typeahead_system.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class SearchQuery {
    @Id
    private String query;
    private long searchCount;
    private Instant lastSearchedAt;

    public SearchQuery() {}

    public SearchQuery(String query, long searchCount, Instant lastSearchedAt) {
        this.query = query;
        this.searchCount = searchCount;
        this.lastSearchedAt = lastSearchedAt;
    }
}