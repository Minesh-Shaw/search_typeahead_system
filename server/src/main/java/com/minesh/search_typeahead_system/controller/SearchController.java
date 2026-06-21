package com.minesh.search_typeahead_system.controller;

import com.minesh.search_typeahead_system.service.SearchService;
import com.minesh.search_typeahead_system.cache.ConsistentHashRouter;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin // Allow frontend access
public class SearchController {

    private final SearchService searchService;
    private final ConsistentHashRouter router;

    public SearchController(SearchService searchService, ConsistentHashRouter router) {
        this.searchService = searchService;
        this.router = router;
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam("q") String prefix) {
        return searchService.getSuggestions(prefix);
    }

    @PostMapping("/search")
    public Map<String, String> search(@RequestBody Map<String, String> payload) {
        searchService.logSearch(payload.get("query"));
        return Map.of("message", "Searched");
    }

    @GetMapping("/cache/debug")
    public Map<String, String> debugCache(@RequestParam("prefix") String prefix) {
        return Map.of(
                "prefix", prefix,
                "routedNodeId", router.routeNode(prefix).getNodeId()
        );
    }

    @GetMapping("/trending")
    public List<String> getTrending() {
        return searchService.getTrending();
    }
}