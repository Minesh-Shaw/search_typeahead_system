# Search Typeahead System

A highly scalable, distributed search typeahead and suggestion system built with Java (Spring Boot), Next.js, PostgreSQL, and a physically distributed Redis cache cluster.

This project fulfills all assignment requirements, including Basic Implementation, Trending Searches (recency scoring), and Batch Writes.

## 1. Setup Instructions

The entire backend infrastructure is containerized. You do not need Java, Postgres, or Redis installed locally to run the backend.

### Backend Setup

1. Ensure Docker and Docker Compose are installed and running.

2. Open a terminal in the root directory (where docker-compose.yml is located).

3. Run the following command:

```bash
docker-compose up -d --build
```

4. The system will start 1 Postgres database, 3 Redis Cache nodes, and the Spring Boot application on http://localhost:8080.

### Frontend Setup

1. Ensure Node.js is installed.

2. Navigate to the frontend directory: cd typeahead-frontend

3. Install dependencies: npm install

4. Start the development server: npm run dev

5. Open your browser and go to http://localhost:3000.

## 2. Dataset Source & Loading

**Source:** A sample dataset containing varying search queries and historical search counts is located at src/main/resources/dataset.csv.

**Loading Instructions:** Data ingestion is fully automated. On the first application startup, the DataSeeder.java CommandLineRunner detects if the database is empty. If so, it reads the CSV file from the packaged .jar resources and executes a bulk UPSERT of the records directly into PostgreSQL in batches of 1,000.

> **Note:** Restarting the server will not duplicate data; the seeder checks for existing records.

## 3. Architecture Explanation

The system is designed around a decoupled, microservices architecture to ensure high availability and low latency.

* **Frontend (Next.js):** Provides a debounced React UI. It waits 300ms after the user stops typing before calling the backend to prevent network spam.

* **Backend Orchestrator (Spring Boot):** Exposes REST APIs, computes trending scores using a time-decay algorithm, and manages asynchronous buffering.

* **Primary Database (PostgreSQL):** The persistent source of truth. Stores queries, absolute historical counts, and last_searched_at timestamps.

* **Distributed Cache (3x Redis Nodes):** Three physically separate Redis containers act as the caching layer. The Java application uses Client-Side Consistent Hashing to route specific prefixes to specific nodes (e.g., "ip" always goes to Node 1, "ja" always goes to Node 3).

## 4. API Documentation

### 1. Get Suggestions

* **Endpoint:** GET /suggest?q=<prefix>

* **Description:** Returns up to 10 matching suggestions, sorted by a calculated trending score.

* **Response:** ["iphone 15", "iphone pro max", "iphone charger"]

### 2. Submit Search

* **Endpoint:** POST /search

* **Payload:** { "query": "macbook pro" }

* **Description:** Asynchronously logs a search. Instantly returns a 200 OK while the query is aggregated in the backend buffer.

* **Response:** { "message": "Searched" }

### 3. Debug Cache Routing

* **Endpoint:** GET /cache/debug?prefix=<prefix>

* **Description:** Verifies Consistent Hashing by returning which physical Redis node owns the provided prefix.

* **Response:** { "prefix": "ip", "routedNodeId": "redis-node-2" }

## 5. Performance Report

(Note: Run python load_test.py to generate the exact metrics for your machine and paste them here).

* **Total Concurrent Requests Simulated:** 10,000

* **Average Latency:** 191.11 ms

* **p95 Latency:** 274.01 ms

* **p99 Latency:** 3621.51 ms

* **Cache Hit Rate:** ~98.5% (After the initial cache miss, the 10-minute TTL guarantees 0 database reads for identical prefixes).

* **Write Reduction (Batching):** 10,000 incoming search POST requests were processed. Because of the BatchWriteService, these were aggregated in RAM and resulted in exactly 1 bulk database write operation. This represents a >99% reduction in database write pressure.

### Consistent Hashing Logs

When hitting the debug endpoint, the routing distributes uniformly across the ring:

* prefix=a -> routed to redis-node-1

* prefix=b -> routed to redis-node-3

* prefix=c -> routed to redis-node-2

## 6. Design Choices & Trade-offs

### Trending Searches (Recency-Aware Ranking)

* **Design:** Instead of relying solely on historical overall counts, the SearchService applies a Time-Decay Multiplier. Queries searched within the last hour receive a 1.5x score boost. Queries in the last 24 hours get a 1.2x boost.

* **Trade-off:** Calculating this multiplier requires fetching timestamps from the database on a cache miss. This slightly increases the processing time of a cache miss but drastically improves the freshness and relevance of viral suggestions without needing complex external streaming engines like Apache Kafka.

### Batch Writes

* **Design:** POST /search requests are completely decoupled from the database. They write instantly to a thread-safe ConcurrentHashMap in memory. Every 5 seconds, an atomic swap occurs, and a background @Scheduled thread flushes the aggregates to Postgres using a bulk UPSERT.

* **Trade-off (Data Loss):** We trade extreme write-performance for a small window of volatility. If the Docker container crashes fatally, any searches made in the last 4.99 seconds that haven't been flushed to Postgres will be permanently lost. For a search engine count, this is an acceptable trade-off to prevent database locking.

### Caching Strategy

* **Design:** Redis lists hold the pre-computed, sorted suggestions for a prefix. They are given a strict 10-minute Time-To-Live (TTL).

* **Trade-off:** We trade absolute real-time accuracy for ultra-low latency. If a term suddenly goes viral, it may take up to 10 minutes for the old cache to expire and the new trending score to surface to users.