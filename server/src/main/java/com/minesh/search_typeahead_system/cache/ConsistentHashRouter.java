package com.minesh.search_typeahead_system.cache;

import java.util.SortedMap;
import java.util.TreeMap;

// The Consistent Hashing Logic (Core Assignment Requirement)
public class ConsistentHashRouter {
    private final SortedMap<Integer, CacheNode> ring = new TreeMap<>();

    public void addNode(CacheNode node, int virtualNodes) {
        for (int i = 0; i < virtualNodes; i++) {
            int hash = (node.getNodeId() + "-VN" + i).hashCode();
            ring.put(hash, node);
        }
    }

    public CacheNode routeNode(String key) {
        if (ring.isEmpty()) return null;
        int hash = key.hashCode();
        if (!ring.containsKey(hash)) {
            SortedMap<Integer, CacheNode> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }
}