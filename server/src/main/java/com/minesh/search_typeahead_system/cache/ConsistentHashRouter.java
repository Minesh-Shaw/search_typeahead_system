package com.minesh.search_typeahead_system.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRouter {
    private final SortedMap<Integer, CacheNode> ring = new TreeMap<>();

    public void addNode(CacheNode node, int virtualNodes) {
        for (int i = 0; i < virtualNodes; i++) {
            // Use the improved hash function for virtual nodes
            int hash = getHash(node.getNodeId() + "-VN" + i);
            ring.put(hash, node);
        }
    }

    public CacheNode routeNode(String key) {
        if (ring.isEmpty()) return null;
        
        // Use the improved hash function for the search prefix
        int hash = getHash(key);
        
        if (!ring.containsKey(hash)) {
            SortedMap<Integer, CacheNode> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }

    // A robust hashing algorithm to evenly distribute short strings
    private int getHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            // Convert the first 4 bytes of the MD5 hash into an Integer
            return ((digest[3] & 0xFF) << 24) |
                   ((digest[2] & 0xFF) << 16) |
                   ((digest[1] & 0xFF) << 8) |
                   (digest[0] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            return key.hashCode(); // Fallback if MD5 is missing
        }
    }
}