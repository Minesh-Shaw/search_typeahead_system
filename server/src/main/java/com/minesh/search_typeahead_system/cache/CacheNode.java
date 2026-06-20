package com.minesh.search_typeahead_system.cache;

import java.util.List;

public interface CacheNode {
    String getNodeId();
    void putSuggestions(String prefix, List<String> suggestions);
    List<String> getSuggestions(String prefix);
}